package com.example.spiral_event_lottery_app.ui.details

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.example.spiral_event_lottery_app.data.TagRepository
import com.example.spiral_event_lottery_app.ui.event_creation.QRCodeActivity
import com.example.spiral_event_lottery_app.ui.events.PosterAdapter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.launch

/**
 * Fragment that displays the details of a specific event.
 * Now supports mandatory geolocation check when joining a waitlist.
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
    private val tagRepository = TagRepository()
    private var eventListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null

    private var userInterested: List<String> = emptyList()
    private var userNotInterested: List<String> = emptyList()

    private lateinit var interestsChipGroup: ChipGroup
    private var currentEventInterests: String = ""
    private var isGeolocationRequired: Boolean = false
    
    private var detailsTitle: TextView? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            captureLocationAndJoin()
        } else {
            Toast.makeText(requireContext(), "Location permission is required to join this event.", Toast.LENGTH_LONG).show()
        }
    }

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
        val backBtn = view.findViewById<ImageButton>(R.id.backButton)
        detailsTitle = view.findViewById<TextView>(R.id.detailsTitle)
        val locationName = view.findViewById<TextView>(R.id.detailsLocation)
        val locationAddress = view.findViewById<TextView>(R.id.detailsLocationAddress)
        val time = view.findViewById<TextView>(R.id.detailsTime)
        val waiting = view.findViewById<TextView>(R.id.detailsWaiting)
        val description = view.findViewById<TextView>(R.id.detailsDescription)
        val posterViewPager = view.findViewById<ViewPager2>(R.id.eventPosterViewPager)
        val posterIndicator = view.findViewById<TabLayout>(R.id.posterIndicator)
        val joinBtn = view.findViewById<Button>(R.id.joinLeaveButton)
        val viewQRBtn = view.findViewById<ImageButton>(R.id.viewQRButtonIcon)
        interestsChipGroup = view.findViewById(R.id.detailsInterestsChipGroup)
        val commentsBtn = view.findViewById<Button>(R.id.commentsButton)

        val editPosterBtn = view.findViewById<View?>(R.id.editImageButton)

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        viewQRBtn.setOnClickListener {
            val intent = Intent(requireContext(), QRCodeActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            intent.putExtra("EVENT_NAME", detailsTitle?.text.toString())
            startActivity(intent)
        }

        commentsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, com.example.spiral_event_lottery_app.ui.comments.EventCommentsFragment.newInstance(eventId, false))
                .addToBackStack(null)
                .commit()
        }

        startUserListening()
        detailsTitle?.let { title ->
            startEventListening(title, locationName, locationAddress, time, waiting, description, posterViewPager, posterIndicator, joinBtn, editPosterBtn)
        }
    }
    /**
     * Listens to the user's document to keep interests in sync in real-time.
     */
    private fun startUserListening() {
        val uid = DeviceIdProvider.getDeviceId(requireContext())
        userListener = FirebaseFirestore.getInstance().collection("users").document(uid)
            .addSnapshotListener { doc, _ ->
                if (isAdded && doc != null && doc.exists()) {
                    userInterested = doc.get("interested") as? List<String> ?: emptyList()
                    userNotInterested = doc.get("notInterested") as? List<String> ?: emptyList()
                    populateInterests(interestsChipGroup, currentEventInterests)
                }
            }
    }

    private fun startEventListening(
        title: TextView,
        locationName: TextView,
        locationAddress: TextView,
        time: TextView,
        waiting: TextView,
        description: TextView,
        posterViewPager: ViewPager2,
        posterIndicator: TabLayout,
        joinBtn: Button,
        editPosterBtn: View?
    ) {
        eventListener = repository.listenToEvent(
            eventId,
            { event ->
                if (!isAdded || event == null) {
                    title.text = "Event not found"
                    return@listenToEvent
                }

                isGeolocationRequired = "Enable".equals(event.geolocation, ignoreCase = true)
                val currentEventName = event.name
                title.text = event.name
                locationName.text = event.locationName
                locationAddress.text = event.locationName
                time.text = event.timeText

                val openSpots = event.maxEntrants?.minus(event.waitingCount.toInt()) ?: 0
                waiting.text = if (event.maxEntrants != null && !event.lotteryDone) {
                    "${event.waitingCount} People on Waiting List, $openSpots Open Spots"
                } else {
                    "${event.waitingCount} People on Waiting List"
                }

                description.text = if (event.description.isNullOrEmpty()) "No description available" else event.description

                if (currentEventInterests != event.interests) {
                    currentEventInterests = event.interests
                    populateInterests(interestsChipGroup, currentEventInterests)
                }

                val currentUserId = DeviceIdProvider.getDeviceId(requireContext())
                editPosterBtn?.let { btn ->
                    btn.visibility = if (event.organizerId == currentUserId) View.VISIBLE else View.GONE
                    btn.setOnClickListener { imagePickerLauncher.launch("image/*") }
                }

                val posters = event.posterUriStrings.ifEmpty {
                    if (event.posterUriString != null) listOf(event.posterUriString!!) else emptyList()
                }

                if (posters.isNotEmpty()) {
                    posterViewPager.adapter = PosterAdapter(posters)
                    TabLayoutMediator(posterIndicator, posterViewPager) { _, _ -> }.attach()
                    posterIndicator.visibility = if (posters.size > 1) View.VISIBLE else View.GONE
                } else {
                    posterViewPager.adapter = PosterAdapter(listOf(""))
                    posterIndicator.visibility = View.GONE
                }

                repository.isJoined(eventId, { joined ->
                    if (!isAdded) return@isJoined
                    joinBtn.visibility = View.VISIBLE
                    if (joined) {
                        joinBtn.text = "You're in the waiting list"
                        joinBtn.backgroundTintList = ColorStateList.valueOf(Color.RED)
                    } else {
                        joinBtn.text = "Join Waiting List"
                        joinBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E5A27"))
                    }
                }, {})

                joinBtn.setOnClickListener {
                    repository.isJoined(eventId, { joined ->
                        if (!isAdded) return@isJoined
                        if (joined) {
                            showSimpleDialog("Already registered", "You're already on the waiting list for $currentEventName.")
                        } else {
                            repository.isSelected(eventId, { selected ->
                                if (!isAdded) return@isSelected
                                if (selected) {
                                    showSimpleDialog("Already Selected", "You have already been selected for $currentEventName.")
                                } else {
                                    handleJoinRequest(event.name)
                                }
                            }, {})
                        }
                    }, {})
                }
            },
            { e -> if (isAdded) Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
        )
    }

    private fun handleJoinRequest(eventName: String) {
        if (isGeolocationRequired) {
            AlertDialog.Builder(requireContext())
                .setTitle("Location Required")
                .setMessage("The organizer requires your location to join this event's waitlist. Do you agree to share your current coordinates?")
                .setPositiveButton("Agree") { _, _ -> checkPermissionAndJoin() }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            showJoinConfirmation(eventName, null)
        }
    }

    private fun checkPermissionAndJoin() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            captureLocationAndJoin()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun captureLocationAndJoin() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                showJoinConfirmation(detailsTitle?.text.toString() ?: "Event", location)
            } else {
                Toast.makeText(requireContext(), "Could not retrieve location. Please ensure GPS is on.", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to get location.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showJoinConfirmation(currentEventName: String, location: Location?) {
        AlertDialog.Builder(requireContext())
            .setTitle("Waitlist Confirmation")
            .setMessage("Successfully join the waiting list for $currentEventName?\n\n• Entry is random\n• You may leave at any time")
            .setPositiveButton("Confirm") { _, _ ->
                repository.joinWaitlist(eventId, location?.latitude, location?.longitude, {
                    if (isAdded) {
                        NotificationManager.sendNotification(
                            DeviceIdProvider.getDeviceId(requireContext()),
                            "Requested", "Your entry for $currentEventName was received!",
                            "REQUESTED", currentEventName, eventId
                        )
                        Toast.makeText(requireContext(), "Joined successfully!", Toast.LENGTH_SHORT).show()
                    }
                }, {}, { e ->
                    if (isAdded) Toast.makeText(context, e.message ?: "Join failed", Toast.LENGTH_LONG).show()
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Populates the ChipGroup with tags.
     * Uses a coroutine to fetch tag details and highlight them if they match user interests.
     */
    private fun populateInterests(chipGroup: ChipGroup, interests: String) {
        if (interests.isEmpty()) {
            chipGroup.removeAllViews()
            return
        }

        val tags = interests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        // Use a set of tags to fetch to avoid redundant network calls via TagRepository cache
        lifecycleScope.launch {
            tagRepository.fetchAndCacheTags(tags)
            
            if (!isAdded) return@launch
            chipGroup.removeAllViews()
            
            for (tag in tags) {
                val chip = Chip(requireContext())
                chip.text = tag
                chip.isCheckable = true
                chip.isClickable = false
                chip.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_interest_state_list)
                chip.chipStrokeColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_interest_stroke_state_list)
                chip.chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width_custom)

                val tagInfo = tagRepository.getTagImmediate(tag)
                
                // HIGHLIGHT LOGIC: Highlight if user is interested in the tag directly OR its parent categories
                val isDirectInterested = userInterested.any { it.equals(tag, ignoreCase = true) }
                val isParentInterested = tagInfo.parents.any { parent -> 
                    userInterested.any { it.equals(parent, ignoreCase = true) }
                }
                
                chip.isChecked = isDirectInterested || isParentInterested
                chipGroup.addView(chip)
            }
        }
    }

    private fun showSimpleDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext()).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    }

    private fun uploadPoster(uri: Uri) {
        val storageRef = FirebaseStorage.getInstance().getReference("event_posters/${eventId}_${System.currentTimeMillis()}.jpg")
        storageRef.putFile(uri).continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            storageRef.downloadUrl
        }.addOnSuccessListener { downloadUri: Uri ->
            FirebaseFirestore.getInstance().collection("events").document(eventId).update("posterUriString", downloadUri.toString())
        }
    }

    override fun onStop() {
        super.onStop()
        eventListener?.remove()
        userListener?.remove()
    }
}
