package com.example.spiral_event_lottery_app.ui.details

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.example.spiral_event_lottery_app.ui.comments.EventCommentsFragment
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage

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

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadPoster(it) }
    }
    
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            saveLocationToFirestore(eventId, DeviceIdProvider.getDeviceId(requireContext()))
        }
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
        val title = view.findViewById<TextView>(R.id.detailsTitle)
        val locationName = view.findViewById<TextView>(R.id.detailsLocation)
        val time = view.findViewById<TextView>(R.id.detailsTime)
        val waiting = view.findViewById<TextView>(R.id.detailsWaiting)
        val description = view.findViewById<TextView>(R.id.detailsDescription)
        val posterImage = view.findViewById<ImageView>(R.id.eventPosterImage)
        val joinBtn = view.findViewById<Button>(R.id.joinLeaveButton)
        val commentsBtn = view.findViewById<Button>(R.id.commentsButton)
        val viewQRBtn = view.findViewById<ImageButton>(R.id.viewQRButtonIcon)
        val editPosterBtn = view.findViewById<View?>(R.id.editImageButton)

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }

        commentsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EventCommentsFragment.newInstance(eventId, false))
                .addToBackStack(null)
                .commit()
        }

        viewQRBtn.setOnClickListener {
            val intent = Intent(requireContext(), com.example.event_creation.QRCodeActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            intent.putExtra("EVENT_NAME", title.text.toString())
            startActivity(intent)
        }

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
                time.text = event.timeText

                val limit = event.maxEntrants?.toLong() ?: 0L
                val spots = limit - event.waitingCount
                val openSpots = if (spots > 0) spots else 0L
                
                waiting.text = if (event.maxEntrants != null && !event.lotteryDone) {
                    "${event.waitingCount} People on Waiting List, $openSpots Open Spots"
                } else {
                    "${event.waitingCount} People on Waiting List"
                }

                description.text = if (event.description.isNullOrEmpty()) "No description available" else event.description

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
                    joinBtn.visibility = View.VISIBLE
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
                        }, {})
                    }
                }, {})

                joinBtn.setOnClickListener {
                    repository.isJoined(eventId, { joined ->
                        if (!isAdded) return@isJoined
                        if (joined) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Already registered")
                                .setMessage("You're already on the waiting list for\n$currentEventName.")
                                .setPositiveButton("OK", null)
                                .show()
                        } else {
                            repository.isSelected(eventId, { selected ->
                                if (!isAdded) return@isSelected
                                if (selected) {
                                    showSimpleDialog("Already Selected", "You have already been selected for $currentEventName.")
                                } else {
                                    showJoinConfirmation(currentEventName)
                                }
                            }, {})
                        }
                    }, {})
                }
            },
            { e -> if (isAdded) Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
        )
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
                        captureAndStoreLocation(eventId, DeviceIdProvider.getDeviceId(requireContext()))
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
        FirebaseFirestore.getInstance().collection("events").document(eventId)
            .update("posterUriString", url)
            .addOnSuccessListener {
                if (isAdded) Toast.makeText(requireContext(), "Poster updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Failed to update database: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun captureAndStoreLocation(eventId: String, deviceId: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            saveLocationToFirestore(eventId, deviceId)
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun saveLocationToFirestore(eventId: String, deviceId: String) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val locationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
        fusedClient.getCurrentLocation(locationRequest, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    FirebaseFirestore.getInstance()
                        .collection("events").document(eventId)
                        .collection("waitlist").document(deviceId)
                        .update(mapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude
                        ))
                }
            }
    }

    override fun onStop() {
        super.onStop()
        eventListener?.remove()
    }
}
