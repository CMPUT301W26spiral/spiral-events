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
 * Fragment that displays the details of a specific event and allows an entrant to join the event's waiting list
 * This screen is opened when a user selects "Sign Up"
 * Communicates with EventRepository to retrieve data from Firebase Firestore
 */
class EventDetailsFragment : Fragment() {
    companion object {
        private const val ARG_EVENT_ID = "event_id"

        /**
         * Creates a new instance of EventDetailsFragment for a specific event
         * The event ID is passed as a fragment argument so the fragment can retrieve the correct event
         */
        fun newInstance(eventId: String): EventDetailsFragment {
            return EventDetailsFragment().apply {
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_event_details, container, false)

    /**
     * Initializes the UI and sets up the Firebase listener for the selected event
     * Configures the Join Waiting List button logic and displays confirmation message when the user attempts to join
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = EventRepository(requireContext())

        val backBtn = view.findViewById<ImageButton>(R.id.backButton)
        val title = view.findViewById<TextView>(R.id.detailsTitle)
        val location = view.findViewById<TextView>(R.id.detailsLocation)
        val time = view.findViewById<TextView>(R.id.detailsTime)
        val waiting = view.findViewById<TextView>(R.id.detailsWaiting)
        val joinBtn = view.findViewById<Button>(R.id.joinLeaveButton)

        joinBtn.text = "Join Waiting List"

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        eventListener = repository.listenToEvent(
            eventId,
            { event ->
                if (event == null) {
                    title.text = "Event not found"
                    location.text = ""
                    time.text = ""
                    waiting.text = ""
                    joinBtn.isEnabled = false
                    return@listenToEvent
                }

                title.text = event.name
                location.text = event.locationName
                time.text = event.timeText
                waiting.text = "${event.waitingCount} People on Waiting List"

                joinBtn.setOnClickListener {
                    repository.isJoined(
                        eventId,
                        { joined ->
                            if (joined) {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Already registered")
                                    .setMessage("You're already on the waiting list for\n${event.name}.")
                                    .setPositiveButton("OK", null)
                                    .show()
                            } else {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("You have successfully joined the waiting list for\n${event.name}")
                                    .setMessage(
                                        "Note on Lottery Selection Criteria:\n\n" +
                                                "• Entry is random from the waiting list\n" +
                                                "• If someone declines, another entrant may be selected\n" +
                                                "• Organizers may set eligibility rules"
                                    )
                                    .setPositiveButton("Confirm") { _, _ ->
                                        repository.joinWaitlist(
                                            eventId,
                                            {},
                                            {
                                                AlertDialog.Builder(requireContext())
                                                    .setTitle("Already registered")
                                                    .setMessage("You're already on the waiting list for\n${event.name}.")
                                                    .setPositiveButton("OK", null)
                                                    .show()
                                            },
                                            { e ->
                                                Toast.makeText(requireContext(), e.message ?: "Join failed", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            }
                        },
                        { e ->
                            Toast.makeText(requireContext(), e.message ?: "Failed to check registration", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            },
            { e ->
                Toast.makeText(requireContext(), e.message ?: "Failed to load event", Toast.LENGTH_LONG).show()
            }
        )
    }

    override fun onStop() {
        super.onStop()
        eventListener?.remove()
        eventListener = null
    }
}
