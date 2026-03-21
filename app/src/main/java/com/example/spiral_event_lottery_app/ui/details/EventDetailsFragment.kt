package com.example.spiral_event_lottery_app.ui.details

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage

/**
 * Fragment that displays the details of a specific event.
 * Now supports editing the event poster for organizers.
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
    private var eventListener: ListenerRegistration? = null

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
        val posterImage = view.findViewById<ImageView>(R.id.eventPosterImage)
        val editPosterBtn = view.findViewById<ImageView>(R.id.editImageButton)
        val joinBtn = view.findViewById<Button>(R.id.joinLeaveButton)

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        eventListener = repository.listenToEvent(
            eventId,
            { event ->
                if (event == null) {
                    title.text = "Event not found"
                    return@listenToEvent
                }

                val currentEventName = event.name
                title.text = currentEventName
                locationName.text = event.locationName
                locationAddress.text = event.locationName
                time.text = event.timeText
                
                val openSpots = event.maxEntrants?.minus(event.waitingCount) ?: 0
                waiting.text = "${event.waitingCount} People on Waiting List, $openSpots Open Spots"
                description.text = if (event.description.isNullOrEmpty()) "No description available" else event.description

                // Only allow the organizer to edit the poster
                val currentUserId = DeviceIdProvider.getDeviceId(requireContext())
                editPosterBtn.visibility = if (event.organizerId == currentUserId) View.VISIBLE else View.GONE
                editPosterBtn.setOnClickListener { imagePickerLauncher.launch("image/*") }

                if (!event.posterUriString.isNullOrEmpty()) {
                    Glide.with(this).load(event.posterUriString).placeholder(R.drawable.ic_event).into(posterImage)
                } else {
                    posterImage.setImageResource(R.drawable.ic_event)
                }

                // Check waitlist and selection status whenever event data updates
                repository.isJoined(eventId, { joined ->
                    if (joined) {
                        joinBtn.text = "You're in the waiting list"
                        joinBtn.backgroundTintList = ColorStateList.valueOf(Color.RED)
                    } else {
                        repository.isSelected(eventId, { selected ->
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
                        if (joined) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Already registered")
                                .setMessage("You're already on the waiting list for\n$currentEventName.")
                                .setPositiveButton("OK", null)
                                .show()
                        } else {
                            repository.isSelected(eventId, { selected ->
                                if (selected) {
                                    AlertDialog.Builder(requireContext())
                                        .setTitle("Already Selected")
                                        .setMessage("You have already been selected for $currentEventName.")
                                        .setPositiveButton("OK", null)
                                        .show()
                                } else {
                                    AlertDialog.Builder(requireContext())
                                        .setTitle("Waitlist Confirmation")
                                        .setMessage("Successfully join the waiting list for $currentEventName?\n\n• Entry is random\n• You may leave at any time")
                                        .setPositiveButton("Confirm") { _, _ ->
                                            repository.joinWaitlist(eventId, {
                                                NotificationManager.sendNotification(
                                                    DeviceIdProvider.getDeviceId(requireContext()),
                                                    "Requested",
                                                    "Your entry for $currentEventName was received!",
                                                    "REQUESTED",
                                                    currentEventName,
                                                    eventId
                                                )
                                                joinBtn.text = "You're in the waiting list"
                                                joinBtn.backgroundTintList = ColorStateList.valueOf(Color.RED)
                                            }, {}, { e -> 
                                                Toast.makeText(requireContext(), e.message ?: "Join failed", Toast.LENGTH_LONG).show() 
                                            })
                                        }
                                        .setNegativeButton("Cancel", null)
                                        .show()
                                }
                            }, {})
                        }
                    }, {})
                }
            },
            { e -> Toast.makeText(requireContext(), e.message ?: "Failed to load event", Toast.LENGTH_LONG).show() }
        )
    }

    /**
     * Uploads the selected image to Firebase Storage and updates the Firestore document.
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
     * Updates the 'posterUriString' field in the Firestore 'events' collection.
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
    }
}
