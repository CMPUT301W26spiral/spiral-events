package com.example.spiral_event_lottery_app.ui.details

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
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.EventRepository
import com.google.firebase.firestore.ListenerRegistration

/**
 * Fragment that disoplays the details of an event that they have already joined and allows leave
 * This screen is opened from the My Events page and when the user click the "Details" button for an event they are currently joined
 * Same logic and EventDetailsFragment, just reversed
 */
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_event_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = EventRepository(requireContext())

        val backBtn = view.findViewById<ImageButton>(R.id.backButton)
        val title = view.findViewById<TextView>(R.id.detailsTitle)
        val location = view.findViewById<TextView>(R.id.detailsLocation)
        val time = view.findViewById<TextView>(R.id.detailsTime)
        val waiting = view.findViewById<TextView>(R.id.detailsWaiting)
        val actionBtn = view.findViewById<Button>(R.id.joinLeaveButton)

        actionBtn.text = "Leave Waiting List"
        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        eventListener = repository.listenToEvent(
            eventId,
            { event ->
                if (event == null) {
                    title.text = "Event not found"
                    actionBtn.isEnabled = false
                    return@listenToEvent
                }

                title.text = event.name
                location.text = event.locationName
                time.text = event.timeText
                waiting.text = "${event.waitingCount} People on Waiting List"

                actionBtn.setOnClickListener {
                    repository.isJoined(
                        eventId,
                        { joined ->
                            if (!joined) {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Not registered")
                                    .setMessage("You're not on the waiting list for\n${event.name}.")
                                    .setPositiveButton("OK", null)
                                    .show()
                            } else {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("You have successfully left the waiting list for:\n${event.name}")
                                    .setPositiveButton("Confirm") { _, _ ->
                                        repository.leaveWaitlist(
                                            eventId,
                                            {
                                                parentFragmentManager.popBackStack()
                                            },
                                            {
                                                AlertDialog.Builder(requireContext())
                                                    .setTitle("Not registered")
                                                    .setMessage("You're not on the waiting list for\n${event.name}.")
                                                    .setPositiveButton("OK", null)
                                                    .show()
                                            },
                                            { e ->
                                                Toast.makeText(
                                                    requireContext(),
                                                    e.message ?: "Leave failed",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        )
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            }
                        },
                        { e ->
                            Toast.makeText(
                                requireContext(),
                                e.message ?: "Failed to check registration",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            },
            { e ->
                Toast.makeText(
                    requireContext(),
                    e.message ?: "Failed to load event",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    override fun onStop() {
        super.onStop()
        eventListener?.remove()
        eventListener = null
    }
}