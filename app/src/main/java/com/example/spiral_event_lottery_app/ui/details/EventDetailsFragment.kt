package com.example.spiral_event_lottery_app.ui.details

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage

/**
 * Fragment that displays the details of a specific event.
 * Now supports editing the event poster for organizers and interest selection.
 */
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
    private lateinit var repository: EventRepository
    private var eventListener: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()
    private lateinit var uid: String

    private val interestedList = mutableListOf<String>()
    private val notInterestedList = mutableListOf<String>()
    private val customInterests = mutableSetOf<String>()

    // Register the image picker at the class level
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadPoster(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = requireArguments().getString(ARG_EVENT_ID)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_event_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = EventRepository(requireContext())
        uid = DeviceIdProvider.getDeviceId(requireContext())
        
        val backBtn = view.findViewById<ImageButton>(R.id.backButton)
        val title = view.findViewById<TextView>(R.id.detailsTitle)
        val locationName = view.findViewById<TextView>(R.id.detailsLocation)
        val locationAddress = view.findViewById<TextView>(R.id.detailsLocationAddress)
        val time = view.findViewById<TextView>(R.id.detailsTime)
        val waiting = view.findViewById<TextView>(R.id.detailsWaiting)
        val description = view.findViewById<TextView>(R.id.detailsDescription)
        val posterImage = view.findViewById<ImageView>(R.id.eventPosterImage)
        val joinBtn = view.findViewById<Button>(R.id.joinLeaveButton)
        val viewQRBtn = view.findViewById<ImageButton>(R.id.viewQRButtonIcon)
        val interestsChipGroup = view.findViewById<ChipGroup>(R.id.detailsInterestsChipGroup)

        val editPosterBtn = view.findViewById<View?>(R.id.editImageButton)

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        viewQRBtn.setOnClickListener {
            val intent = Intent(requireContext(), com.example.event_creation.QRCodeActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            intent.putExtra("EVENT_NAME", title.text.toString())
            startActivity(intent)
        }

        // Load user interests first to highlight correctly
        loadUserInterests {
            eventListener = repository.listenToEvent(
                eventId,
                { event ->
                    if (!isAdded || event == null) {
                        title.text = "Event not found"
                        return@listenToEvent
                    }

                    val currentEventName = event.name
                    title.text = currentEventName
                    locationName.text = event.locationName
                    locationAddress.text = event.locationName
                    time.text = event.timeText

                    val limit = event.maxEntrants?.toLong() ?: 0L
                    val spots = limit - event.waitingCount
                    val openSpots = if (spots > 0) spots else 0L

                    waiting.text = "${event.waitingCount} People on Waiting List, $openSpots Open Spots"
                    description.text = if (event.description.isNullOrEmpty()) "No description available" else event.description

                    // Populate interests chips
                    interestsChipGroup.removeAllViews()
                    if (!event.interests.isNullOrEmpty()) {
                        val interestsList = event.interests.split(",").map { it.trim() }
                        for (interest in interestsList) {
                            if (interest.isNotEmpty()) {
                                addInterestChip(interestsChipGroup, interest)
                            }
                        }
                    }

                    val currentUserId = DeviceIdProvider.getDeviceId(requireContext())
                    editPosterBtn?.let { btn ->
                        btn.visibility = if (event.organizerId == currentUserId) View.VISIBLE else View.GONE
                        btn.setOnClickListener { imagePickerLauncher.launch("image/*") }
                    }

                    if (!event.posterUriString.isNullOrEmpty()) {
                        Glide.with(this).load(event.posterUriString).placeholder(R.drawable.ic_event).into(posterImage)
                    } else {
                        posterImage.setImageResource(R.drawable.ic_event)
                    }

                    repository.isJoined(eventId, { joined ->
                        if (!isAdded) return@isJoined
                        if (joined) {
                            joinBtn.text = "You're in the waiting list"
                            joinBtn.backgroundTintList = ColorStateList.valueOf(Color.RED)
                        } else {
                            repository.isSelected(eventId, { selected ->
                                if (!isAdded) return@isSelected
                                if (selected) {
                                    joinBtn.text = "You are selected/invited"
                                    joinBtn.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
                                    joinBtn.isEnabled = false
                                } else {
                                    joinBtn.text = "Join Waiting List"
                                    joinBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E5A27"))
                                    joinBtn.isEnabled = true
                                }
                            }, { })
                        }
                    }, { })

                    joinBtn.setOnClickListener {
                        repository.isJoined(eventId, { joined ->
                            if (!isAdded) return@isJoined
                            if (joined) {
                                showSimpleDialog("Already registered", "You're already on the waiting list for\n$currentEventName.")
                            } else {
                                repository.isSelected(eventId, { selected ->
                                    if (!isAdded) return@isSelected
                                    if (selected) {
                                        showSimpleDialog("Already Selected", "You have already been selected for $currentEventName.")
                                    } else {
                                        showJoinConfirmation(currentEventName)
                                    }
                                }, { })
                            }
                        }, { })
                    }
                },
                { e -> if (isAdded) Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
            )
        }
    }

    private fun loadUserInterests(onComplete: () -> Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val interested = doc.get("interested") as? List<String>
                val notInterested = doc.get("notInterested") as? List<String>
                val custom = doc.get("customInterests") as? List<String>
                interestedList.clear()
                if (interested != null) interestedList.addAll(interested)
                notInterestedList.clear()
                if (notInterested != null) notInterestedList.addAll(notInterested)
                customInterests.clear()
                if (custom != null) customInterests.addAll(custom)
            }
            onComplete()
        }.addOnFailureListener {
            onComplete()
        }
    }

    private fun addInterestChip(group: ChipGroup, interest: String) {
        val chip = Chip(requireContext())
        chip.text = interest
        chip.isClickable = true
        chip.isCheckable = false
        
        updateChipStyle(chip, interest)

        chip.setOnClickListener {
            customInterests.add(interest) // Mark as custom if user interacts with it
            if (!interestedList.contains(interest) && !notInterestedList.contains(interest)) {
                interestedList.add(interest) // Neutral -> Interested
            } else if (interestedList.contains(interest)) {
                interestedList.remove(interest)
                notInterestedList.add(interest) // Interested -> Not Interested
            } else {
                notInterestedList.remove(interest) // Not Interested -> Neutral
            }
            updateChipStyle(chip, interest)
            saveInterestsToFirebase()
        }

        group.addView(chip)
    }

    private fun updateChipStyle(chip: Chip, interest: String) {
        if (interestedList.contains(interest)) {
            chip.setChipBackgroundColorResource(R.color.interest_green_bg)
            chip.setTextColor(Color.BLACK)
        } else if (notInterestedList.contains(interest)) {
            chip.setChipBackgroundColorResource(R.color.interest_red_bg)
            chip.setTextColor(Color.BLACK)
        } else {
            chip.setChipBackgroundColorResource(android.R.color.white)
            chip.setTextColor(Color.BLACK)
        }
    }

    private fun saveInterestsToFirebase() {
        val data = mapOf(
            "interested" to interestedList,
            "notInterested" to notInterestedList,
            "customInterests" to customInterests.toList()
        )
        db.collection("users").document(uid).update(data)
    }

    private fun showSimpleDialog(title: String, message: String) {
        val ctx = context ?: return
        AlertDialog.Builder(ctx).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    }

    private fun showJoinConfirmation(currentEventName: String) {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle("Waitlist Confirmation")
            .setMessage("Successfully join the waiting list for $currentEventName?\n\n• Entry is random\n• You may leave at any time")
            .setPositiveButton("Confirm") { _, _ ->
                repository.joinWaitlist(eventId, {
                    context?.let { safeCtx ->
                        NotificationManager.sendNotification(
                            DeviceIdProvider.getDeviceId(safeCtx),
                            "Requested",
                            "Your entry for $currentEventName was received!",
                            "REQUESTED",
                            currentEventName,
                            eventId
                        )
                        Toast.makeText(safeCtx, "Joined successfully!", Toast.LENGTH_SHORT).show()
                    }
                }, {}, { e ->
                    if (isAdded) Toast.makeText(context, e.message ?: "Join failed", Toast.LENGTH_LONG).show()
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadPoster(uri: Uri) {
        val storageRef = FirebaseStorage.getInstance().getReference("event_posters/${eventId}_${System.currentTimeMillis()}.jpg")
        if (isAdded) Toast.makeText(requireContext(), "Uploading new poster...", Toast.LENGTH_SHORT).show()

        storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                updateFirestorePoster(downloadUri.toString())
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFirestorePoster(url: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("events").document(eventId)
            .update("posterUriString", url)
            .addOnSuccessListener {
                if (isAdded) Toast.makeText(requireContext(), "Poster updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Failed to update database: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStop() {
        super.onStop()
        eventListener?.remove()
    }
}
