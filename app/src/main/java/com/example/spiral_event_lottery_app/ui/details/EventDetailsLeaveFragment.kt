package com.example.spiral_event_lottery_app.ui.details

import android.content.Intent
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
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.example.spiral_event_lottery_app.ui.comments.EventCommentsFragment
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
        val commentsBtn = view.findViewById<Button>(R.id.commentsButton)
        val acceptBtn = view.findViewById<Button>(R.id.acceptInvitationButton)
        val joinedText = view.findViewById<TextView>(R.id.successfullyJoinedText)
        val viewQRBtn = view.findViewById<ImageButton>(R.id.viewQRButtonIcon)

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        commentsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EventCommentsFragment.newInstance(eventId, false))
                .addToBackStack(null)
                .commit()
        viewQRBtn.setOnClickListener {
            val intent = Intent(requireContext(), com.example.event_creation.QRCodeActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            intent.putExtra("EVENT_NAME", title.text.toString())
            startActivity(intent)
        }

        eventListener = repository.listenToEvent(eventId, { event ->
            if (event == null) return@listenToEvent
            title.text = event.name
            location.text = event.locationName
            time.text = event.timeText
            waiting.text = "${event.waitingCount} People on Waiting List"

            // Load the event poster from Firestore URL
            if (!event.posterUriString.isNullOrEmpty()) {
                Glide.with(this)
                    .load(event.posterUriString)
                    .placeholder(R.drawable.ic_event)
                    .into(posterImage)
            } else {
                posterImage.setImageResource(R.drawable.ic_event)
            }

            // Real-time check for entrant status
            repository.getWinnerStatus(eventId) { status ->
                if (!isAdded) return@getWinnerStatus

                // Normalizing status case sensitivity if needed ("Accepted" vs "accepted")
                if ("Accepted".equals(status, ignoreCase = true)) {
                    // Already accepted: Show success message, hide all action buttons
                    joinedText.visibility = View.VISIBLE
                    joinedText.text = "You have accepted the invitation! \uD83C\uDF89"
                    joinedText.setTextColor(resources.getColor(R.color.primary_green, null))
                    acceptBtn.visibility = View.GONE
                    actionBtn.visibility = View.GONE
                } else if ("Declined".equals(status, ignoreCase = true)) {
                    // Show declined status
                    joinedText.visibility = View.VISIBLE
                    joinedText.text = "You declined this invitation"
                    joinedText.setTextColor(android.graphics.Color.RED)
                    acceptBtn.visibility = View.GONE
                    actionBtn.visibility = View.GONE
                } else {
                    // Check if the user is in the canceled_list (declined via redraw logic)
                    repository.getEntrantIds(eventId, "canceled_list", { canceledIds ->
                        if (!isAdded) return@getEntrantIds
                        val myId = DeviceIdProvider.getDeviceId(requireContext())

                        if (canceledIds.contains(myId)) {
                            joinedText.visibility = View.VISIBLE
                            joinedText.text = "You declined this invitation"
                            joinedText.setTextColor(android.graphics.Color.RED)
                            acceptBtn.visibility = View.GONE
                            actionBtn.visibility = View.GONE
                        } else {
                            joinedText.visibility = View.GONE
                            
                            repository.isSelected(eventId, { isWinner ->
                                if (!isAdded) return@isSelected

                                if (isWinner) {
                                    acceptBtn.visibility = View.VISIBLE
                                    actionBtn.visibility = View.VISIBLE
                                    actionBtn.text = "Decline Invitation"

                                    acceptBtn.setOnClickListener {
                                        val handler = acceptanceHandling()
                                        handler.invitation_accepted(requireContext(), eventId, myId)
                                        // The status check in this listener will automatically update the UI
                                    }

                                    actionBtn.setOnClickListener {
                                        AlertDialog.Builder(requireContext())
                                            .setTitle("Decline Invitation")
                                            .setMessage("Are you sure you want to decline? You will not be able to join again.")
                                            .setPositiveButton("Decline") { _, _ ->
                                                repository.declineInvitation(eventId, {
                                                    // Redraw handled in repository.declineInvitation onSuccess
                                                }, { e -> Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show() })
                                            }
                                            .setNegativeButton("Cancel", null)
                                            .show()
                                    }
                                } else {
                                    acceptBtn.visibility = View.GONE
                                    actionBtn.visibility = View.VISIBLE
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
