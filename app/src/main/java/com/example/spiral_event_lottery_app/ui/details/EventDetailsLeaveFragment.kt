package com.example.spiral_event_lottery_app.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.google.firebase.firestore.ListenerRegistration

class EventDetailsLeaveFragment : Fragment() {
    companion object {
        private const val ARG_EVENT_ID = "event_id"
        fun newInstance(eventId: String): EventDetailsLeaveFragment {
            return EventDetailsLeaveFragment().apply {
                arguments = Bundle().apply { putString(ARG_EVENT_ID, eventId) }
            }
        }
    }

    private lateinit var eventId: String
    private lateinit var repository: EventRepository
    private var eventListener: ListenerRegistration? = null

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
        val location = view.findViewById<TextView>(R.id.detailsLocation)
        val time = view.findViewById<TextView>(R.id.detailsTime)
        val waiting = view.findViewById<TextView>(R.id.detailsWaiting)
        val posterImage = view.findViewById<ImageView>(R.id.eventPosterImage)
        val actionBtn = view.findViewById<Button>(R.id.joinLeaveButton)

        actionBtn.text = "Leave Waiting List"
        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        eventListener = repository.listenToEvent(eventId, { event ->
            if (event == null) return@listenToEvent
            title.text = event.name
            location.text = event.locationName
            time.text = event.timeText
            waiting.text = "${event.waitingCount} People on Waiting List"

            if (!event.posterUriString.isNullOrEmpty()) {
                Glide.with(this)
                    .load(event.posterUriString)
                    .placeholder(R.drawable.ic_event)
                    .into(posterImage)
            } else {
                posterImage.setImageResource(R.drawable.ic_event)
            }

            actionBtn.setOnClickListener {
                repository.isJoined(eventId, { joined ->
                    if (!joined) {
                        AlertDialog.Builder(requireContext()).setTitle("Not registered").setMessage("You're not on the waiting list for\n${event.name}.").setPositiveButton("OK", null).show()
                    } else {
                        AlertDialog.Builder(requireContext()).setTitle("Leave Waiting List").setMessage("Are you sure you want to leave the waiting list for ${event.name}?").setPositiveButton("Confirm") { _, _ ->
                            repository.leaveWaitlist(eventId, {
                                val currentUserId = DeviceIdProvider.getDeviceId(requireContext())
                                
                                // 1. Notify the Entrant (Confirmation)
                                NotificationManager.sendNotification(currentUserId, "Cancelled", "You have left the waiting list for ${event.name}.", "DENIED", event.name, eventId)
                                
                                // 2. Notify the Organizer (Update)
                                if (!event.organizerId.isNullOrEmpty()) {
                                    NotificationManager.sendNotification(
                                        event.organizerId,
                                        "Entrant Left",
                                        "An entrant has left the waiting list for your event: ${event.name}.",
                                        "ORGANIZER",
                                        event.name,
                                        eventId
                                    )
                                }
                                
                                parentFragmentManager.popBackStack()
                            }, {}, { e -> Toast.makeText(requireContext(), e.message ?: "Leave failed", Toast.LENGTH_LONG).show() })
                        }.setNegativeButton("Cancel", null).show()
                    }
                }, {})
            }
        }, {})
    }

    override fun onStop() {
        super.onStop()
        eventListener?.remove()
    }
}
