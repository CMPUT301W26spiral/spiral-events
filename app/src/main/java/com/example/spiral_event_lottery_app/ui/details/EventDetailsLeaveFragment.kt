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
import com.example.spiral_event_lottery_app.acceptanceHandling
import com.google.firebase.firestore.ListenerRegistration

/**
 * Fragment that displays event details for joined entrants and winners.
 * Hides action buttons if the invitation has already been accepted or declined.
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
        val acceptBtn = view.findViewById<Button>(R.id.acceptInvitationButton)
        val joinedText = view.findViewById<TextView>(R.id.successfullyJoinedText)

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

            // CHECK STATUS: Only show buttons if the user hasn't already accepted/declined
            repository.getWinnerStatus(eventId) { status ->
                if (!isAdded) return@getWinnerStatus

                if ("Accepted" == status) {
                    // Already accepted: Show success message, hide all action buttons
                    joinedText.visibility = View.VISIBLE
                    joinedText.text = "Successfully joined!"
                    joinedText.setTextColor(resources.getColor(R.color.primary_green, null))
                    acceptBtn.visibility = View.GONE
                    actionBtn.visibility = View.GONE
                } else {
                    // Check if the user is in the canceled_list (declined)
                    repository.getEntrantIds(eventId, "canceled_list", { canceledIds ->
                        if (!isAdded) return@getEntrantIds
                        val myId = DeviceIdProvider.getDeviceId(requireContext())
                        
                        if (canceledIds.contains(myId)) {
                            // User declined: Hide all buttons and show status
                            joinedText.visibility = View.VISIBLE
                            joinedText.text = "You declined this invitation"
                            joinedText.setTextColor(android.graphics.Color.RED)
                            acceptBtn.visibility = View.GONE
                            actionBtn.visibility = View.GONE
                        } else {
                            // Not yet confirmed: Ensure text is hidden
                            joinedText.visibility = View.GONE

                            // Not yet confirmed: Show buttons based on selection status
                            repository.isSelected(eventId, { isWinner ->
                                if (!isAdded) return@isSelected

                                actionBtn.visibility = View.VISIBLE

                                if (isWinner) {
                                    // State: WINNER - show both Accept and Decline
                                    acceptBtn.visibility = View.VISIBLE
                                    actionBtn.text = "Decline Invitation"

                                    acceptBtn.setOnClickListener {
                                        val handler = acceptanceHandling()
                                        handler.invitation_accepted(requireContext(), eventId, DeviceIdProvider.getDeviceId(requireContext()))
                                        parentFragmentManager.popBackStack()
                                    }

                                    actionBtn.setOnClickListener {
                                        AlertDialog.Builder(requireContext())
                                            .setTitle("Decline Invitation")
                                            .setMessage("Are you sure you want to decline? You will not be able to join again.")
                                            .setPositiveButton("Decline") { _, _ ->
                                                repository.declineInvitation(eventId, {
                                                    repository.triggerAutomaticRedraw(eventId, event.name)
                                                    parentFragmentManager.popBackStack()
                                                }, { e -> Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show() })
                                            }
                                            .setNegativeButton("Cancel", null)
                                            .show()
                                    }
                                } else {
                                    // State: ENTRANT - only show Leave button
                                    acceptBtn.visibility = View.GONE
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
                        }
                    }, {})
                }
            }
        }, {})
    }

    override fun onStop() {
        super.onStop()
        eventListener?.remove()
    }
}
