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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = requireArguments().getString(ARG_EVENT_ID)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_event_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val backBtn = view.findViewById<ImageButton>(R.id.backButton)
        val title = view.findViewById<TextView>(R.id.detailsTitle)
        val location = view.findViewById<TextView>(R.id.detailsLocation)
        val time = view.findViewById<TextView>(R.id.detailsTime)
        val waiting = view.findViewById<TextView>(R.id.detailsWaiting)
        val actionBtn = view.findViewById<Button>(R.id.joinLeaveButton)

        val event = EventStore.allEvents.firstOrNull { it.id == eventId }
        if (event == null) {
            title.text = "Event not found"
            actionBtn.visibility = View.GONE
            backBtn.setOnClickListener { parentFragmentManager.popBackStack() }
            return
        }

        title.text = event.name
        location.text = event.locationName
        time.text = event.timeText
        waiting.text = "${event.waitingCount} People on Waiting List"

        // This screen is for leaving
        actionBtn.text = "Leave Waiting List"

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        actionBtn.setOnClickListener {
            if (!EventStore.isJoined(eventId)) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Not registered")
                    .setMessage("You're not on the waiting list for\n${event.name}.")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }

            // Confirm leave
            AlertDialog.Builder(requireContext())
                .setTitle("You have successfully left the waiting list for:\n${event.name}")
                .setPositiveButton("Confirm") { _, _ ->
                    EventStore.leave(eventId)

                    // Pop back to My Events screen
                    parentFragmentManager.popBackStack()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}