package com.example.spiral_event_lottery_app.ui.details

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
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
import com.example.event_creation.QRCodeActivity
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.google.firebase.firestore.ListenerRegistration

/**
 * Fragment that displays the details of a specific event.
 */
class EventDetailsFragment : Fragment() {
    companion object {
        private const val ARG_EVENT_ID = "event_id"
        
        @JvmStatic
        fun newInstance(eventId: String): EventDetailsFragment {
            return EventDetailsFragment().apply {
                arguments = Bundle().apply { putString(ARG_EVENT_ID, eventId) }
            }
        }
    }

    private lateinit var eventId: String
    private lateinit var repository: EventRepository
    private var eventListener: ListenerRegistration? = null
    private var currentEventName: String? = null

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
        val joinBtn = view.findViewById<Button>(R.id.joinLeaveButton)
        val qrBtn = view.findViewById<Button>(R.id.viewQrCodeButton)

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        qrBtn.setOnClickListener {
            val intent = Intent(requireContext(), QRCodeActivity::class.java).apply {
                putExtra("EVENT_ID", eventId)
                putExtra("EVENT_NAME", currentEventName)
            }
            startActivity(intent)
        }

        eventListener = repository.listenToEvent(
            eventId,
            { event ->
                if (event == null) {
                    title.text = "Event not found"
                    return@listenToEvent
                }

                currentEventName = event.name
                title.text = currentEventName
                locationName.text = event.locationName
                locationAddress.text = event.locationName
                time.text = event.timeText
                
                val openSpots = event.maxEntrants?.minus(event.waitingCount) ?: 0
                waiting.text = "${event.waitingCount} People on Waiting List, $openSpots Open Spots"
                description.text = if (event.description.isNullOrEmpty()) "No description available" else event.description

                if (!event.posterUriString.isNullOrEmpty()) {
                    Glide.with(this).load(event.posterUriString).placeholder(R.drawable.ic_event).into(posterImage)
                } else {
                    posterImage.setImageResource(R.drawable.ic_event)
                }

                // Check join status whenever event data updates
                repository.isJoined(eventId, { joined ->
                    if (joined) {
                        joinBtn.text = "You're in the waiting list"
                        joinBtn.backgroundTintList = ColorStateList.valueOf(Color.RED)
                    } else {
                        joinBtn.text = "Join Waiting List"
                        joinBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E5A27"))
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
            },
            { e -> Toast.makeText(requireContext(), e.message ?: "Failed to load event", Toast.LENGTH_LONG).show() }
        )
    }

    override fun onStop() {
        super.onStop()
        eventListener?.remove()
    }
}
