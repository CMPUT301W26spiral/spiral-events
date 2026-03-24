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
import com.example.spiral_event_lottery_app.data.EventRepository
import com.google.firebase.firestore.ListenerRegistration

/**
 * Fragment that displays event details for joined entrants and winners.
 * Implements automated redrawing when a winner declines their invitation.
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

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        eventListener = repository.listenToEvent(eventId, { event ->
            if (event == null) return@listenToEvent
            title.text = event.name
            location.text = event.locationName
            time.text = event.timeText
            waiting.text = "${event.waitingCount} People on Waiting List"

            if (!event.posterUriString.isNullOrEmpty()) {
                Glide.with(this).load(event.posterUriString).placeholder(R.drawable.ic_event).into(posterImage)
            } else {
                posterImage.setImageResource(R.drawable.ic_event)
            }

            // Determine if user is a winner or just a waitlist entrant
            repository.isSelected(eventId, { isWinner ->
                if (isWinner) {
                    actionBtn.text = "Decline Invitation"
                    actionBtn.setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Decline Invitation")
                            .setMessage("Are you sure you want to decline? You will not be able to join again.")
                            .setPositiveButton("Decline") { _, _ ->
                                repository.declineInvitation(eventId, {
                                    // TRIGGER AUTOMATION: A spot opened up, so pull a new person
                                    repository.triggerAutomaticRedraw(eventId, event.name)
                                    parentFragmentManager.popBackStack()
                                }, { e -> Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show() })
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                } else {
                    actionBtn.text = "Leave Waiting List"
                    actionBtn.setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Leave Waiting List")
                            .setMessage("Are you sure you want to leave the waiting list?")
                            .setPositiveButton("Confirm") { _, _ ->
                                repository.leaveWaitlist(eventId, {
                                    parentFragmentManager.popBackStack()
                                }, {}, { e -> Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show() })
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }, {})
        }, {})
    }

    override fun onStop() {
        super.onStop()
        eventListener?.remove()
    }
}
