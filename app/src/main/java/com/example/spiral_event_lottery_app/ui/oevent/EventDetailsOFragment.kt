package com.example.spiral_event_lottery_app.ui.oevent

import android.app.AlertDialog
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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.model.User
import com.example.spiral_event_lottery_app.ui.odetails.DoDrawFragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage

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
    private var eventListener: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()

    // UI elements
    private lateinit var title: TextView
    private lateinit var locationName: TextView
    private lateinit var time: TextView
    private lateinit var waiting: TextView
    private lateinit var description: TextView
    private lateinit var posterImage: ImageView
    private lateinit var inviteHeader: TextView
    private lateinit var inviteRow: View
    private lateinit var inviteSearchInput: EditText
    private lateinit var inviteCategorySpinner: Spinner
    private lateinit var searchResultRecycler: RecyclerView
    private lateinit var searchAdapter: UserSearchAdapter

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
        posterImage = view.findViewById(R.id.eventPosterImage)
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

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        editPosterBtn.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        deleteEventBtn.setOnClickListener {
            showDeleteEventDialog()
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

        // Draw Navigation
        drawBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DoDrawFragment.newInstance(eventId))
                .addToBackStack(null)
                .commit()
        }

        // Entrants List Navigation
        viewEntrantsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, com.example.spiral_event_lottery_app.ui.organizer_view.ManageEntrantsFragment.newInstance(eventId))
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onStart() {
        super.onStart()
        startEventListener()
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

                if (!event.posterUriString.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(event.posterUriString)
                        .placeholder(R.drawable.ic_event)
                        .into(posterImage)
                } else {
                    posterImage.setImageResource(R.drawable.ic_event)
                }
            },
            { e ->
                if (isAdded) Toast.makeText(requireContext(), e.message ?: "Failed to load event", Toast.LENGTH_LONG).show()
            }
        )
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
            val currentCount = eventDoc.getLong("waiting_count") ?: 0L
            
            transaction.set(waitlistRef, waitlistData)
            transaction.update(eventRef, "waiting_count", currentCount + 1)
            null
        }.addOnSuccessListener {
            Toast.makeText(requireContext(), "${user.name} has been added to the waiting list!", Toast.LENGTH_SHORT).show()
            inviteSearchInput.text.clear()
            searchAdapter.submitList(emptyList())
            searchResultRecycler.visibility = View.GONE
        }.addOnFailureListener { e ->
            val msg = if (e.message == "ALREADY_IN_WAITLIST") "User is already on the waitlist." else "Failed to invite: ${e.message}"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Performs a Firestore search for users matching the query string.
     * Limits search results to the first 5 matches.
     * @param query The search string (name, email, or phone).
     */
    private fun performUserSearch(query: String) {
        val category = inviteCategorySpinner.selectedItem.toString().lowercase()
        val field = when(category) {
            "name" -> "name"
            "email" -> "email"
            "phone" -> "phoneNumber"
            else -> "name"
        }

        db.collection("users")
            .whereGreaterThanOrEqualTo(field, query)
            .whereLessThanOrEqualTo(field, query + "\uf8ff")
            .limit(5)
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                searchAdapter.submitList(users)
                searchResultRecycler.visibility = if (users.isNotEmpty()) View.VISIBLE else View.GONE
            }
    }

    /**
     * Uploads a local image file to Firebase Storage.
     * On success, updates the Firestore document with the new image URL.
     * @param uri The local URI of the image to upload.
     */
    private fun uploadPoster(uri: Uri) {
        val storageRef = FirebaseStorage.getInstance().getReference("event_posters/${eventId}_${System.currentTimeMillis()}.jpg")
        Toast.makeText(requireContext(), "Uploading new poster...", Toast.LENGTH_SHORT).show()

        storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                updateFirestorePoster(downloadUri.toString())
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Updates the Firestore event document with a new poster image URL.
     * @param url The public download URL of the new poster.
     */
    private fun updateFirestorePoster(url: String) {
        FirebaseFirestore.getInstance().collection("events").document(eventId)
            .update("posterUriString", url)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Poster updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update database: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStop() {
        super.onStop()
        eventListener?.remove()
        eventListener = null
    }

    /**
     * Adapter for displaying matching entrants in the search dropdown.
     */
    private inner class UserSearchAdapter(private val onItemClick: (User) -> Unit) :
        RecyclerView.Adapter<UserSearchAdapter.VH>() {
        private var users = listOf<User>()

        fun submitList(newList: List<User>) {
            users = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user_search_result, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val user = users[position]
            holder.userName.text = user.name
            holder.userDetail.text = user.email
            holder.itemView.setOnClickListener { onItemClick(user) }
        }

        override fun getItemCount() = users.size
        
        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userDetail: TextView = itemView.findViewById(R.id.userDetail)
        }
    }
}
