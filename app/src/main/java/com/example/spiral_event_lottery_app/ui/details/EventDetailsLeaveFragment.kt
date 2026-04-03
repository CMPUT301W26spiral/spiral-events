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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.acceptanceHandling
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.example.spiral_event_lottery_app.data.TagRepository
import com.example.spiral_event_lottery_app.ui.events.PosterAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

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
    private val tagRepository = TagRepository()
    private var eventListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null

    private var userInterested: List<String> = emptyList()
    private var currentEventInterests: String = ""

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
        val address = view.findViewById<TextView>(R.id.detailsLocationAddress)
        val time = view.findViewById<TextView>(R.id.detailsTime)
        val waiting = view.findViewById<TextView>(R.id.detailsWaiting)
        val posterViewPager = view.findViewById<ViewPager2>(R.id.eventPosterViewPager)
        val posterIndicator = view.findViewById<TabLayout>(R.id.posterIndicator)
        val actionBtn = view.findViewById<Button>(R.id.joinLeaveButton)
        val acceptBtn = view.findViewById<Button>(R.id.acceptInvitationButton)
        val joinedText = view.findViewById<TextView>(R.id.successfullyJoinedText)
        val viewQRBtn = view.findViewById<ImageButton>(R.id.viewQRButtonIcon)
        val commentsBtn = view.findViewById<Button>(R.id.commentsButton)
        val interestsChipGroup = view.findViewById<ChipGroup>(R.id.detailsInterestsChipGroup)

        actionBtn.text = "Leave Waiting List"
        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        viewQRBtn.setOnClickListener {
            val intent = Intent(requireContext(), com.example.event_creation.QRCodeActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            intent.putExtra("EVENT_NAME", title.text.toString())
            startActivity(intent)
        }

        commentsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, com.example.spiral_event_lottery_app.ui.comments.EventCommentsFragment.newInstance(eventId, false))
                .addToBackStack(null)
                .commit()
        }

        startUserListening(interestsChipGroup)

        eventListener = repository.listenToEvent(eventId, { event ->
            if (event == null || !isAdded) return@listenToEvent
            title.text = event.name
            location.text = event.locationName
            address.text = event.locationName
            time.text = event.timeText
            waiting.text = "${event.waitingCount} People on Waiting List"

            currentEventInterests = event.interests
            populateInterests(interestsChipGroup, currentEventInterests)

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

            val myId = DeviceIdProvider.getDeviceId(requireContext())

            // Real-time check for entrant status
            repository.getWinnerStatus(eventId) { status ->
                if (!isAdded) return@getWinnerStatus

                if ("accepted".equals(status, ignoreCase = true)) {
                    joinedText.visibility = View.VISIBLE
                    joinedText.text = "You have accepted the invitation! \uD83C\uDF89"
                    joinedText.setTextColor(resources.getColor(R.color.primary_green, null))
                    acceptBtn.visibility = View.GONE
                    actionBtn.visibility = View.GONE
                } else if ("declined".equals(status, ignoreCase = true)) {
                    joinedText.visibility = View.VISIBLE
                    joinedText.text = "You declined this invitation"
                    joinedText.setTextColor(android.graphics.Color.RED)
                    acceptBtn.visibility = View.GONE
                    actionBtn.visibility = View.GONE
                } else if (status != null) {
                    // This means they are in selected_list but haven't accepted/declined yet
                    acceptBtn.visibility = View.VISIBLE
                    actionBtn.visibility = View.VISIBLE
                    actionBtn.text = "Decline Invitation"

                    acceptBtn.setOnClickListener {
                        val handler = acceptanceHandling()
                        handler.invitation_accepted(requireContext(), eventId, myId)
                    }

                    actionBtn.setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Decline Invitation")
                            .setMessage("Are you sure you want to decline? You will not be able to join again.")
                            .setPositiveButton("Decline") { _, _ ->
                                repository.declineInvitation(eventId, {
                                    // Status update will trigger UI refresh
                                }, { e -> Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show() })
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                } else {
                    // Check if they are in the canceled_list
                    repository.getEntrantIds(eventId, "canceled_list", { canceledIds ->
                        if (!isAdded) return@getEntrantIds
                        if (canceledIds.contains(myId)) {
                            joinedText.visibility = View.VISIBLE
                            joinedText.text = "You declined this invitation"
                            joinedText.setTextColor(android.graphics.Color.RED)
                            acceptBtn.visibility = View.GONE
                            actionBtn.visibility = View.GONE
                        } else {
                            // Check waitlist status (could be pending for private event)
                            repository.getWaitlistStatus(eventId) { waitlistStatus ->
                                if (!isAdded) return@getWaitlistStatus
                                
                                if ("pending".equals(waitlistStatus, ignoreCase = true)) {
                                    joinedText.visibility = View.GONE
                                    acceptBtn.visibility = View.VISIBLE
                                    actionBtn.visibility = View.VISIBLE
                                    actionBtn.text = "Decline Private Invitation"
                                    
                                    acceptBtn.setOnClickListener {
                                        repository.acceptPrivateInvitation(eventId, {
                                            Toast.makeText(requireContext(), "Invitation accepted!", Toast.LENGTH_SHORT).show()
                                        }, { e -> Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show() })
                                    }
                                    
                                    actionBtn.setOnClickListener {
                                        AlertDialog.Builder(requireContext())
                                            .setTitle("Decline Invitation")
                                            .setMessage("Are you sure you want to decline this private invitation?")
                                            .setPositiveButton("Decline") { _, _ ->
                                                repository.declinePrivateInvitation(eventId, {
                                                    parentFragmentManager.popBackStack()
                                                }, { e -> Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show() })
                                            }
                                            .setNegativeButton("Cancel", null)
                                            .show()
                                    }
                                } else {
                                    // Normal waitlist behavior
                                    joinedText.visibility = View.GONE
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
                            }
                        }
                    }, {})
                }
            }
        }, {})
    }
    private fun startUserListening(chipGroup: ChipGroup) {
        val uid = DeviceIdProvider.getDeviceId(requireContext())
        userListener = FirebaseFirestore.getInstance().collection("users").document(uid)
            .addSnapshotListener { doc, _ ->
                if (isAdded && doc != null && doc.exists()) {
                    userInterested = doc.get("interested") as? List<String> ?: emptyList()
                    populateInterests(chipGroup, currentEventInterests)
                }
            }
    }


    private fun populateInterests(chipGroup: ChipGroup, interests: String) {
        val tags = interests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        chipGroup.removeAllViews()
        if (interests.isEmpty()) return

        for (tag in tags) {
            val chip = Chip(requireContext())
            chip.text = tag
            chip.isCheckable = true
            chip.isClickable = false

            chip.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_interest_state_list)
            chip.chipStrokeColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_interest_stroke_state_list)
            chip.chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width_custom)

            lifecycleScope.launch {
                val tagInfo = tagRepository.getTag(tag)
                val isDirectInterested = userInterested.contains(tag)
                val isParentInterested = tagInfo?.parents?.any { userInterested.contains(it) } ?: false
                if (isAdded) {
                    chip.isChecked = isDirectInterested || isParentInterested
                }
            }
            chipGroup.addView(chip)
        }
    }

    override fun onStop() {
        super.onStop()
        eventListener?.remove()
        userListener?.remove()
    }
}
