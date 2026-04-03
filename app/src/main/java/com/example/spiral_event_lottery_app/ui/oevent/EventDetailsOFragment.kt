package com.example.spiral_event_lottery_app.ui.oevent

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.data.TagRepository
import com.example.spiral_event_lottery_app.model.User
import com.example.spiral_event_lottery_app.ui.event_creation.QRCodeActivity
import com.example.spiral_event_lottery_app.ui.events.PosterAdapter
import com.example.spiral_event_lottery_app.ui.odetails.DoDrawFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

/**
 * Fragment that displays the details of a specific event from an organizer's perspective.
 * Supports editing the event poster and private event specific UI including entrant search.
 */
class EventDetailsOFragment : Fragment() {
    companion object {
        private const val ARG_EVENT_ID = "event_id"

        /**
         * Creates a new instance of EventDetailsOFragment with the given event ID.
         * @param eventId The unique identifier of the event.
         * @return A new instance of this fragment.
         */
        fun newInstance(eventId: String): EventDetailsOFragment {
            return EventDetailsOFragment().apply {
                arguments = Bundle().apply { putString(ARG_EVENT_ID, eventId) }
            }
        }
    }

    private lateinit var eventId: String
    private lateinit var repository: EventRepository
    private val tagRepository = TagRepository()
    private var eventListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()

    private var userInterested: List<String> = emptyList()

    // UI elements
    private lateinit var title: TextView
    private lateinit var locationName: TextView
    private lateinit var time: TextView
    private lateinit var waiting: TextView
    private lateinit var description: TextView
    private lateinit var posterViewPager: ViewPager2
    private lateinit var posterIndicator: TabLayout
    private lateinit var interestsChipGroup: ChipGroup
    private lateinit var inviteHeader: TextView
    private lateinit var inviteRow: View
    private lateinit var inviteSearchInput: EditText
    private lateinit var inviteCategorySpinner: Spinner
    private lateinit var searchResultRecycler: RecyclerView
    private lateinit var searchAdapter: UserSearchAdapter
    private var currentEventInterests: String = ""

    // Register the image picker at the class level
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadPoster(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = requireArguments().getString(ARG_EVENT_ID)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_event_details_o, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = EventRepository(requireContext())

        // Initialize UI elements
        val backBtn = view.findViewById<ImageButton>(R.id.backButton)
        title = view.findViewById(R.id.detailsTitle)
        locationName = view.findViewById(R.id.detailsLocation)
        time = view.findViewById(R.id.detailsTime)
        waiting = view.findViewById(R.id.detailsWaiting)
        description = view.findViewById(R.id.detailsDescription)
        posterViewPager = view.findViewById(R.id.eventPosterViewPager)
        posterIndicator = view.findViewById(R.id.posterIndicator)
        interestsChipGroup = view.findViewById(R.id.detailsInterestsChipGroup)
        val editPosterBtn = view.findViewById<ImageView>(R.id.editImageButton)
        
        inviteHeader = view.findViewById(R.id.inviteHeader)
        inviteRow = view.findViewById(R.id.inviteRow)
        inviteSearchInput = view.findViewById(R.id.inviteSearchInput)
        inviteCategorySpinner = view.findViewById(R.id.inviteCategorySpinner)
        searchResultRecycler = view.findViewById(R.id.searchResultRecycler)

        // Setup search results recycler
        searchAdapter = UserSearchAdapter { user ->
            showInviteDialog(user)
        }
        searchResultRecycler.layoutManager = LinearLayoutManager(requireContext())
        searchResultRecycler.adapter = searchAdapter

        // Setup category spinner
        val categories = listOf("Name", "Email", "Phone")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        inviteCategorySpinner.adapter = adapter

        // Buttons
        val drawBtn = view.findViewById<Button>(R.id.drawButton)
        val viewEntrantsBtn = view.findViewById<Button>(R.id.viewEntrantsButton)
        val notifyEntrantsBtn = view.findViewById<Button>(R.id.notifyEntrantsButton)
        val viewLocationsBtn = view.findViewById<Button>(R.id.viewLocButton)
        val deleteEventBtn = view.findViewById<Button>(R.id.deleteEventButton)
        val viewQRBtn = view.findViewById<ImageButton>(R.id.viewQRButtonIcon)
        val commentsBtn = view.findViewById<Button>(R.id.commentsButton)

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        editPosterBtn.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        deleteEventBtn.setOnClickListener {
            showDeleteEventDialog()
        }

        viewQRBtn.setOnClickListener {
            val intent = Intent(requireContext(), QRCodeActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            intent.putExtra("EVENT_NAME", title.text.toString().removeSuffix(" (Private)"))
            startActivity(intent)
        }

        commentsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, com.example.spiral_event_lottery_app.ui.comments.EventCommentsFragment.newInstance(eventId, true))
                .addToBackStack(null)
                .commit()
        }

        // Implement Search Functionality
        inviteSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length >= 1) {
                    performUserSearch(query)
                } else {
                    searchAdapter.submitList(emptyList())
                    searchResultRecycler.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        drawBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DoDrawFragment.newInstance(eventId))
                .addToBackStack(null)
                .commit()
        }

        viewEntrantsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, com.example.spiral_event_lottery_app.ui.organizer_view.ManageEntrantsFragment.newInstance(eventId))
                .addToBackStack(null)
                .commit()
        }

        startUserListening()
    }

    override fun onStart() {
        super.onStart()
        startEventListener()
    }

    /**
     * Listens to the user's document to keep interests in sync in real-time.
     */
    private fun startUserListening() {
        val uid = DeviceIdProvider.getDeviceId(requireContext())
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { doc, _ ->
                if (isAdded && doc != null && doc.exists()) {
                    userInterested = doc.get("interested") as? List<String> ?: emptyList()
                    populateInterests(interestsChipGroup, currentEventInterests)
                }
            }
    }

    /**
     * Sets up a real-time Firestore listener for the event document.
     * Updates the UI automatically when event data changes.
     */
    private fun startEventListener() {
        eventListener?.remove() // Ensure no duplicate listeners
        eventListener = repository.listenToEvent(
            eventId,
            { event ->
                if (event == null || !isAdded) return@listenToEvent

                if (!event.isPublic) {
                    title.text = "${event.name} (Private)"
                    inviteHeader.visibility = View.VISIBLE
                    inviteRow.visibility = View.VISIBLE
                } else {
                    title.text = event.name
                    inviteHeader.visibility = View.GONE
                    inviteRow.visibility = View.GONE
                }

                locationName.text = event.locationName
                time.text = event.timeText
                
                // Logic for open spots calculation
                val openSpots = event.maxEntrants?.minus(event.waitingCount) ?: 0
                waiting.text = if (event.maxEntrants != null) {
                    "${event.waitingCount} People on Waiting List, $openSpots Open Spots"
                } else {
                    "${event.waitingCount} People on Waiting List"
                }
                
                description.text = if (event.description.isNullOrEmpty()) "No description available" else event.description

                // Populate interests
                currentEventInterests = event.interests
                populateInterests(interestsChipGroup, currentEventInterests)

                val posters = event.posterUriStrings.ifEmpty {
                    if (event.posterUriString != null) listOf(event.posterUriString!!) else emptyList()
                }

                if (posters.isNotEmpty()) {
                    posterViewPager.adapter = PosterAdapter(posters)
                    TabLayoutMediator(posterIndicator, posterViewPager) { _, _ -> }.attach()
                    posterIndicator.visibility = if (posters.size > 1) View.VISIBLE else View.GONE
                } else {
                    posterViewPager.adapter = PosterAdapter(listOf("")) // placeholder
                    posterIndicator.visibility = View.GONE
                }
            },
            { e ->
                if (isAdded) Toast.makeText(requireContext(), e.message ?: "Failed to load event", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun populateInterests(chipGroup: ChipGroup, interests: String) {
        val tags = interests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        chipGroup.removeAllViews()
        if (interests.isEmpty()) return
        
        for (tag in tags) {
            val chip = Chip(requireContext())
            chip.text = tag
            chip.isCheckable = true
            chip.isClickable = false // User cannot toggle from here
            
            // Set styles to match green theme when checked
            chip.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_interest_state_list)
            chip.chipStrokeColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_interest_stroke_state_list)
            chip.chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width_custom)
            
            // If the interest or any of its parents are in the user's list, it shows as selected
            lifecycleScope.launch {
                val tagInfo = tagRepository.getTag(tag)
                val isDirectInterested = userInterested.contains(tag)
                val isParentInterested = tagInfo?.parents?.any { userInterested.contains(it) } ?: false
                
                if (isAdded) {
                    chip.isChecked = isDirectInterested || isParentInterested
                }
            }
            
            chipGroup.addView(chip)
        }
    }

    /**
     * Shows a confirmation dialog for deleting the current event.
     */
    private fun showDeleteEventDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteEvent()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Deletes the current event from Firestore and returns to the previous screen.
     */
    private fun deleteEvent() {
        db.collection("events").document(eventId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Event deleted successfully", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to delete event: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Shows a confirmation dialog for inviting a specific user to the event.
     * @param user The user to potentially invite.
     */
    private fun showInviteDialog(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Invite Entrant")
            .setMessage("Invite ${user.name} to event?")
            .setPositiveButton("Yes") { _, _ ->
                inviteUserToEvent(user)
            }
            .setNegativeButton("No", null)
            .show()
    }

    /**
     * Adds a user to the event's waitlist in Firestore.
     * Uses a transaction to ensure waitlist count accuracy.
     * @param user The user to add to the waitlist.
     */
    private fun inviteUserToEvent(user: User) {
        val waitlistRef = db.collection("events").document(eventId).collection("waitlist").document(user.deviceId)
        
        val waitlistData = hashMapOf(
            "device_id" to user.deviceId,
            "joined_at" to Timestamp.now()
        )

        db.runTransaction { transaction ->
            val waitlistDoc = transaction.get(waitlistRef)
            if (waitlistDoc.exists()) {
                throw Exception("ALREADY_IN_WAITLIST")
            }
            
            val eventRef = db.collection("events").document(eventId)
            val eventDoc = transaction.get(eventRef)
            val currentCount = eventDoc.getLong("waiting_count") ?: 0
            
            transaction.set(waitlistRef, waitlistData)
            transaction.update(eventRef, "waiting_count", currentCount + 1)
        }
        .addOnSuccessListener {
            Toast.makeText(requireContext(), "User invited successfully", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            if (e.message == "ALREADY_IN_WAITLIST") {
                Toast.makeText(requireContext(), "User is already on waitlist", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to invite: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performUserSearch(query: String) {
        val category = inviteCategorySpinner.selectedItem.toString()
        val field = when(category) {
            "Email" -> "email"
            "Phone" -> "phone_number"
            else -> "name"
        }

        db.collection("users")
            .whereGreaterThanOrEqualTo(field, query)
            .whereLessThanOrEqualTo(field, query + "\uf8ff")
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val users = documents.mapNotNull { it.toObject(User::class.java) }
                searchAdapter.submitList(users)
                searchResultRecycler.visibility = if (users.isNotEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun uploadPoster(uri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference.child("event_posters/$eventId.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    db.collection("events").document(eventId)
                        .update("posterUriString", downloadUri.toString())
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Poster updated", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        eventListener?.remove()
        userListener?.remove()
    }
}
