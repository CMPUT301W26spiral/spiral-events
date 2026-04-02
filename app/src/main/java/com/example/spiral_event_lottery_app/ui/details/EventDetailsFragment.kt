package com.example.spiral_event_lottery_app.ui.details

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.example.spiral_event_lottery_app.data.TagRepository
import com.example.spiral_event_lottery_app.ui.events.PosterAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

/**
 * Fragment that displays the details of a specific event.
 * Now supports editing the event poster for organizers.
 * Interests display highlights if they match user preferences but cannot be toggled here.
 */
class EventDetailsFragment : Fragment() {
    companion object {
        private const val ARG_EVENT_ID = "event_id"
        fun newInstance(eventId: String): EventDetailsFragment {
            return EventDetailsFragment().apply {
                arguments = Bundle().apply { putString(ARG_EVENT_ID, eventId) }
            }
        }
    }

    private lateinit var eventId: String
    private lateinit var repository: EventRepository
    private val tagRepository = TagRepository()
    private var eventListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    
    private var userInterested: List<String> = emptyList()
    private var userNotInterested: List<String> = emptyList()

    private lateinit var interestsChipGroup: ChipGroup
    private var currentEventInterests: String = ""

    // Register the image picker at the class level
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadPoster(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = requireArguments().getString(ARG_EVENT_ID)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_event_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = EventRepository(requireContext())
        val backBtn = view.findViewById<ImageButton>(R.id.backButton)
        val title = view.findViewById<TextView>(R.id.detailsTitle)
        val locationName = view.findViewById<TextView>(R.id.detailsLocation)
        val locationAddress = view.findViewById<TextView>(R.id.detailsLocationAddress)
        val time = view.findViewById<TextView>(R.id.detailsTime)
        val waiting = view.findViewById<TextView>(R.id.detailsWaiting)
        val description = view.findViewById<TextView>(R.id.detailsDescription)
        val posterViewPager = view.findViewById<ViewPager2>(R.id.eventPosterViewPager)
        val posterIndicator = view.findViewById<TabLayout>(R.id.posterIndicator)
        val joinBtn = view.findViewById<Button>(R.id.joinLeaveButton)
        val viewQRBtn = view.findViewById<ImageButton>(R.id.viewQRButtonIcon)
        interestsChipGroup = view.findViewById(R.id.detailsInterestsChipGroup)
        val commentsBtn = view.findViewById<Button>(R.id.commentsButton)

        // SAFETY: Use a nullable reference for the edit button which might be missing in XML
        val editPosterBtn = view.findViewById<View?>(R.id.editImageButton)

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        viewQRBtn.setOnClickListener {
            val intent = Intent(requireContext(), com.example.event_creation.QRCodeActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            intent.putExtra("EVENT_NAME", title.text.toString())
            startActivity(intent)
        }

        commentsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, com.example.spiral_event_lottery_app.ui.comments.EventCommentsFragment.newInstance(eventId, false))
                .addToBackStack(null)
                .commit()
        }

        startUserListening()
        startEventListening(title, locationName, locationAddress, time, waiting, description, posterViewPager, posterIndicator, joinBtn, editPosterBtn)
    }

    /**
     * Listens to the user's document to keep interests in sync in real-time.
     */
    private fun startUserListening() {
        val uid = DeviceIdProvider.getDeviceId(requireContext())
        userListener = FirebaseFirestore.getInstance().collection("users").document(uid)
            .addSnapshotListener { doc, _ ->
                if (isAdded && doc != null && doc.exists()) {
                    userInterested = doc.get("interested") as? List<String> ?: emptyList()
                    userNotInterested = doc.get("notInterested") as? List<String> ?: emptyList()
                    // Refresh chips to reflect new selection state
                    populateInterests(interestsChipGroup, currentEventInterests)
                }
            }
    }

    private fun startEventListening(
        title: TextView,
        locationName: TextView,
        locationAddress: TextView,
        time: TextView,
        waiting: TextView,
        description: TextView,
        posterViewPager: ViewPager2,
        posterIndicator: TabLayout,
        joinBtn: Button,
        editPosterBtn: View?
    ) {
        eventListener = repository.listenToEvent(
            eventId,
            { event ->
                if (!isAdded || event == null) {
                    title.text = "Event not found"
                    return@listenToEvent
                }

                val currentEventName = event.name
                title.text = currentEventName
                locationName.text = event.locationName
                locationAddress.text = event.locationName
                time.text = event.timeText

                val limit = event.maxEntrants?.toLong() ?: 0L
                val spots = limit - event.waitingCount
                val openSpots = if (spots > 0) spots else 0L

                waiting.text = "${event.waitingCount} People on Waiting List, $openSpots Open Spots"
                description.text = if (event.description.isNullOrEmpty()) "No description available" else event.description

                currentEventInterests = event.interests
                populateInterests(interestsChipGroup, currentEventInterests)

                val currentUserId = DeviceIdProvider.getDeviceId(requireContext())
                editPosterBtn?.let { btn ->
                    btn.visibility = if (event.organizerId == currentUserId) View.VISIBLE else View.GONE
                    btn.setOnClickListener { imagePickerLauncher.launch("image/*") }
                }

                val posters = event.posterUriStrings.ifEmpty {
                    if (event.posterUriString != null) listOf(event.posterUriString!!) else emptyList()
                }

                if (posters.isNotEmpty()) {
                    posterViewPager.adapter = PosterAdapter(posters)
                    TabLayoutMediator(posterIndicator, posterViewPager) { _, _ -> }.attach()
                    posterIndicator.visibility = if (posters.size > 1) View.VISIBLE else View.GONE
                } else {
                    posterViewPager.adapter = PosterAdapter(listOf(""))
                    posterIndicator.visibility = View.GONE
                }

                repository.isJoined(eventId, { joined ->
                    if (!isAdded) return@isJoined
                    if (joined) {
                        joinBtn.text = "You're in the waiting list"
                        joinBtn.backgroundTintList = ColorStateList.valueOf(Color.RED)
                    } else {
                        repository.isSelected(eventId, { selected ->
                            if (!isAdded) return@isSelected
                            if (selected) {
                                joinBtn.text = "You are selected/invited"
                                joinBtn.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
                                joinBtn.isEnabled = false
                            } else {
                                joinBtn.text = "Join Waiting List"
                                joinBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E5A27"))
                                joinBtn.isEnabled = true
                            }
                        }, {})
                    }
                }, {})

                joinBtn.setOnClickListener {
                    repository.isJoined(eventId, { joined ->
                        if (!isAdded) return@isJoined
                        if (joined) {
                            showSimpleDialog("Already registered", "You're already on the waiting list for\n$currentEventName.")
                        } else {
                            repository.isSelected(eventId, { selected ->
                                if (!isAdded) return@isSelected
                                if (selected) {
                                    showSimpleDialog("Already Selected", "You have already been selected for $currentEventName.")
                                } else {
                                    showJoinConfirmation(currentEventName)
                                }
                            }, {})
                        }
                    }, {})
                }
            },
            { e -> if (isAdded) Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
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

    private fun showSimpleDialog(title: String, message: String) {
        val ctx = context ?: return
        AlertDialog.Builder(ctx).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    }

    private fun showJoinConfirmation(currentEventName: String) {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle("Waitlist Confirmation")
            .setMessage("Successfully join the waiting list for $currentEventName?\n\n• Entry is random\n• You may leave at any time")
            .setPositiveButton("Confirm") { _, _ ->
                repository.joinWaitlist(eventId, {
                    context?.let { safeCtx ->
                        NotificationManager.sendNotification(
                            DeviceIdProvider.getDeviceId(safeCtx),
                            "Requested",
                            "Your entry for $currentEventName was received!",
                            "REQUESTED",
                            currentEventName,
                            eventId
                        )
                        Toast.makeText(safeCtx, "Joined successfully!", Toast.LENGTH_SHORT).show()
                    }
                }, {}, { e ->
                    if (isAdded) Toast.makeText(context, e.message ?: "Join failed", Toast.LENGTH_LONG).show()
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadPoster(uri: Uri) {
        val storageRef = FirebaseStorage.getInstance().getReference("event_posters/${eventId}_${System.currentTimeMillis()}.jpg")
        if (isAdded) Toast.makeText(requireContext(), "Uploading new poster...", Toast.LENGTH_SHORT).show()

        storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                updateFirestorePoster(downloadUri.toString())
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFirestorePoster(url: String) {
        FirebaseFirestore.getInstance().collection("events").document(eventId)
            .update("posterUriString", url)
            .addOnSuccessListener {
                if (isAdded) Toast.makeText(requireContext(), "Poster updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Failed to update database: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStop() {
        super.onStop()
        eventListener?.remove()
        userListener?.remove()
    }
}
