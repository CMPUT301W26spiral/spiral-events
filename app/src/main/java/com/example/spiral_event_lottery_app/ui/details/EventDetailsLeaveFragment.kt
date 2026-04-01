package com.example.spiral_event_lottery_app.ui.details

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.example.spiral_event_lottery_app.ui.events.PosterAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
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
        val posterViewPager = view.findViewById<ViewPager2>(R.id.eventPosterViewPager)
        val posterIndicator = view.findViewById<TabLayout>(R.id.posterIndicator)
        val actionBtn = view.findViewById<Button>(R.id.joinLeaveButton)
        val viewQRBtn = view.findViewById<ImageButton>(R.id.viewQRButtonIcon)

        actionBtn.text = "Leave Waiting List"
        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        viewQRBtn.setOnClickListener {
            val intent = Intent(requireContext(), com.example.event_creation.QRCodeActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            intent.putExtra("EVENT_NAME", title.text.toString())
            startActivity(intent)
        }

        eventListener = repository.listenToEvent(eventId, { event ->
            if (event == null || !isAdded) return@listenToEvent
            title.text = event.name
            location.text = event.locationName
            time.text = event.timeText
            waiting.text = "${event.waitingCount} People on Waiting List"

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

            actionBtn.setOnClickListener {
                repository.isJoined(eventId, { joined ->
                    if (!joined) {
                        AlertDialog.Builder(requireContext()).setTitle("Not registered").setMessage("You're not on the waiting list for\n${event.name}.").setPositiveButton("OK", null).show()
                    } else {
                        AlertDialog.Builder(requireContext()).setTitle("You have successfully left the waiting list for ${event.name}").setPositiveButton("Confirm") { _, _ ->
                            repository.leaveWaitlist(eventId, {
                                NotificationManager.sendNotification(DeviceIdProvider.getDeviceId(requireContext()), "Cancelled", "You have left the waiting list.", "DENIED", event.name, eventId)
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
