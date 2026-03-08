package com.example.spiral_event_lottery_app.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.EventStore

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = requireArguments().getString(ARG_EVENT_ID)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_event_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val backBtn = view.findViewById<ImageButton>(R.id.backButton)
        val title = view.findViewById<TextView>(R.id.detailsTitle)
        val location = view.findViewById<TextView>(R.id.detailsLocation)
        val time = view.findViewById<TextView>(R.id.detailsTime)
        val waiting = view.findViewById<TextView>(R.id.detailsWaiting)
        val joinBtn = view.findViewById<Button>(R.id.joinLeaveButton)

        val event = EventStore.allEvents.firstOrNull { it.id == eventId }
        if (event == null) {
            title.text = "Event not found"
            // Keep button text visible even in error state
            joinBtn.text = "Join Waiting List"
            backBtn.setOnClickListener { parentFragmentManager.popBackStack() }
            joinBtn.setOnClickListener(null)
            return
        }

        fun refreshWaitingCount() {
            val updated = EventStore.allEvents.firstOrNull { it.id == eventId }
            val count = updated?.waitingCount ?: event.waitingCount
            waiting.text = "$count People on Waiting List"
        }

        fun refreshJoinUi() {
            joinBtn.text = "Join Waiting List"
        }

        title.text = event.name
        location.text = event.locationName
        time.text = event.timeText
        refreshWaitingCount()
        refreshJoinUi()

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        joinBtn.setOnClickListener {

            if (EventStore.isJoined(eventId)) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Already registered")
                    .setMessage("You're already on the waiting list for\n${event.name}.")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("You have successfully joined the waiting list for\n${event.name}")
                .setMessage(
                    "Note on Lottery Selection Criteria:\n\n" +
                            "• Entry is random from the waiting list\n" +
                            "• If someone declines, another entrant may be selected\n" +
                            "• Organizers may set eligibility rules"
                )
                .setPositiveButton("Confirm") { _, _ ->
                    EventStore.join(eventId)
                    refreshWaitingCount()
                    refreshJoinUi()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}