package com.example.spiral_event_lottery_app.ui.organizer_view

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.google.firebase.firestore.FirebaseFirestore

/**
 * ManageEntrantsFragment displays and manages entrants for a specific event.
 *
 * Shows three tabs:
 * - Waiting: entrants on the waitlist (not yet drawn)
 * - Invited: entrants drawn from the lottery, with Pending or Accepted status badges
 * - Cancelled: entrants who declined or were cancelled by the organizer
 *
 * User Stories covered:
 * - US 02.02.01: View waiting list
 * - US 02.06.01: View invited/chosen entrants
 * - US 02.06.02: View cancelled entrants
 * - US 02.06.03: View final enrolled entrants (Accepted)
 * - US 02.06.04: Cancel entrants who did not sign up
 * - US 02.06.05: Export final entrant list as CSV
 * - US 02.07.01/02/03: Send notifications to entrants by tab
 * - US 01.05.06: Invite specific entrant to private event
 */
class ManageEntrantsFragment : Fragment(R.layout.fragment_manage_entrants) {

    private lateinit var eventId: String
    private val db = FirebaseFirestore.getInstance()

    private lateinit var invitedRecycler: RecyclerView
    private lateinit var waitingRecycler: RecyclerView
    private lateinit var cancelledRecycler: RecyclerView
    private lateinit var btnInvited: Button
    private lateinit var btnWaiting: Button
    private lateinit var btnCancelled: Button
    private lateinit var waitingCountText: TextView
    private lateinit var btnNotifyAll: Button
    private lateinit var btnInvitePrivate: Button
    private lateinit var btnDrawReplacement: Button
    private lateinit var btnExportCsv: Button

    // Tracks which tab is currently visible
    private var currentTab = "waiting"

    // Cached entrant data for export: Pair(name, status)
    private var invitedEntrants: List<Pair<String, String>> = emptyList()

    companion object {
        /**
         * Creates a new instance of ManageEntrantsFragment with the given event ID.
         *
         * @param eventId The Firestore document ID of the event to manage
         * @return A configured ManageEntrantsFragment instance
         */
        fun newInstance(eventId: String): ManageEntrantsFragment {
            return ManageEntrantsFragment().apply {
                arguments = Bundle().apply { putString("event_id", eventId) }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        eventId = arguments?.getString("event_id") ?: ""

        btnInvitePrivate = view.findViewById(R.id.btnInvitePrivate)
        btnNotifyAll     = view.findViewById(R.id.btnNotifyAll)
        btnInvited       = view.findViewById(R.id.btnInvited)
        btnWaiting       = view.findViewById(R.id.btnWaiting)
        btnCancelled     = view.findViewById(R.id.btnCancelled)
        invitedRecycler  = view.findViewById(R.id.invitedRecycler)
        waitingRecycler  = view.findViewById(R.id.waitingRecycler)
        cancelledRecycler = view.findViewById(R.id.cancelledRecycler)
        waitingCountText = view.findViewById(R.id.waitingCountText)
        btnDrawReplacement = view.findViewById(R.id.btnDrawReplacement)
        btnExportCsv     = view.findViewById(R.id.btnExportCsv)

        invitedRecycler.layoutManager  = LinearLayoutManager(requireContext())
        waitingRecycler.layoutManager  = LinearLayoutManager(requireContext())
        cancelledRecycler.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<View>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnInvited.setOnClickListener   { showTab("invited") }
        btnWaiting.setOnClickListener   { showTab("waiting") }
        btnCancelled.setOnClickListener { showTab("cancelled") }

        btnNotifyAll.setOnClickListener     { notifyAllInCurrentTab() }
        btnInvitePrivate.setOnClickListener { showInviteDialog() }
        btnDrawReplacement.setOnClickListener { }

        // US 02.06.05: Export the final enrolled (Accepted) entrants as CSV
        btnExportCsv.setOnClickListener { exportEnrolledAsCsv() }

        showTab("waiting")
        loadWaitingEntrants()
        loadInvitedEntrants()
        loadCancelledEntrants()
    }

    // -------------------------------------------------------------------------
    // Tab switching
    // -------------------------------------------------------------------------

    /**
     * Switches the visible RecyclerView to the selected tab and updates button styling.
     *
     * @param tab One of "invited", "waiting", or "cancelled"
     */
    private fun showTab(tab: String) {
        currentTab = tab
        invitedRecycler.visibility  = if (tab == "invited")   View.VISIBLE else View.GONE
        waitingRecycler.visibility  = if (tab == "waiting")   View.VISIBLE else View.GONE
        cancelledRecycler.visibility = if (tab == "cancelled") View.VISIBLE else View.GONE

        btnNotifyAll.text = "Notify All ${tab.replaceFirstChar { it.uppercase() }} Entrants"

        val activeColor   = ContextCompat.getColor(requireContext(), R.color.primary_green)
        val inactiveColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val activeText    = ContextCompat.getColor(requireContext(), android.R.color.white)
        val inactiveText  = ContextCompat.getColor(requireContext(), R.color.primary_green)

        listOf(btnInvited, btnWaiting, btnCancelled).forEach { btn ->
            btn.setBackgroundColor(inactiveColor)
            btn.setTextColor(inactiveText)
        }
        val activeBtn = when (tab) {
            "invited"   -> btnInvited
            "waiting"   -> btnWaiting
            else        -> btnCancelled
        }
        activeBtn.setBackgroundColor(activeColor)
        activeBtn.setTextColor(activeText)
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    /**
     * Loads entrants from the waitlist subcollection and displays them.
     * Implements US 02.02.01.
     */
    private fun loadWaitingEntrants() {
        db.collection("events").document(eventId).collection("waitlist").get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.id }
                waitingCountText.text = "${deviceIds.size} People on Waiting List"
                resolveNames(deviceIds) { names ->
                    // Waiting entrants have no status badge
                    val pairs = names.map { Pair(it, "") }
                    waitingRecycler.adapter = EntrantAdapter(pairs)
                }
            }
    }

    /**
     * Loads entrants from selected_list, showing Pending and Accepted with badges.
     * The remove button allows the organizer to cancel a Pending entrant (US 02.06.04).
     * Implements US 02.06.01 and US 02.06.03.
     */
    private fun loadInvitedEntrants() {
        db.collection("events").document(eventId).collection("selected_list")
            .whereIn("Status", listOf("Pending", "Accepted"))
            .get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.id }
                val statusMap = snapshot.documents.associate { doc ->
                    doc.id to (doc.getString("Status") ?: "Pending")
                }
                resolveNames(deviceIds) { names ->
                    val pairs = deviceIds.zip(names).map { (deviceId, name) ->
                        Pair(name, statusMap[deviceId] ?: "Pending")
                    }
                    invitedEntrants = pairs
                    invitedRecycler.adapter = EntrantAdapter(pairs, onRemove = { name ->
                        // Find the deviceId that matches this name so we can cancel them
                        val index = names.indexOf(name)
                        if (index != -1) cancelEntrant(deviceIds[index])
                    })
                }
            }
    }

    /**
     * Loads entrants from selected_list with Declined or Cancelled status.
     * Implements US 02.06.02.
     */
    private fun loadCancelledEntrants() {
        db.collection("events").document(eventId).collection("selected_list")
            .whereIn("Status", listOf("Declined", "Cancelled"))
            .get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.id }
                val statusMap = snapshot.documents.associate { doc ->
                    doc.id to (doc.getString("Status") ?: "Cancelled")
                }
                resolveNames(deviceIds) { names ->
                    val pairs = deviceIds.zip(names).map { (deviceId, name) ->
                        Pair(name, statusMap[deviceId] ?: "Cancelled")
                    }
                    cancelledRecycler.adapter = EntrantAdapter(pairs)
                }
            }
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     * Cancels a specific entrant by setting their Status to "Cancelled" in selected_list.
     * This implements US 02.06.04 (cancel entrants who did not sign up).
     *
     * After cancellation the lists are reloaded so both Invited and Cancelled tabs update.
     *
     * @param deviceId The device ID of the entrant to cancel
     */
    private fun cancelEntrant(deviceId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Entrant")
            .setMessage("Are you sure you want to cancel this entrant?")
            .setPositiveButton("Yes") { _, _ ->
                db.collection("events").document(eventId)
                    .collection("selected_list").document(deviceId)
                    .update("Status", "Cancelled")
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Entrant cancelled.", Toast.LENGTH_SHORT).show()
                        // Reload both tabs so the entrant moves from Invited → Cancelled
                        loadInvitedEntrants()
                        loadCancelledEntrants()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to cancel entrant.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    /**
     * Exports the final list of enrolled (Accepted) entrants to a CSV file.
     * The file is saved to the device's Downloads folder.
     * Implements US 02.06.05.
     */
    private fun exportEnrolledAsCsv() {
        db.collection("events").document(eventId).collection("selected_list")
            .whereEqualTo("Status", "Accepted")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(requireContext(), "No enrolled entrants to export.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val deviceIds = snapshot.documents.map { it.id }
                resolveNames(deviceIds) { names ->
                    // Build CSV content: header + one row per enrolled entrant
                    val csvBuilder = StringBuilder()
                    csvBuilder.appendLine("Name,Device ID,Status")
                    deviceIds.zip(names).forEach { (deviceId, name) ->
                        csvBuilder.appendLine("$name,$deviceId,Accepted")
                    }

                    // Write to Downloads using MediaStore (no permissions needed on Android 10+)
                    try {
                        val fileName = "enrolled_entrants_$eventId.csv"
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }
                        val resolver = requireContext().contentResolver
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { it.write(csvBuilder.toString().toByteArray()) }
                            contentValues.clear()
                            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)
                            Toast.makeText(requireContext(), "CSV saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(), "Failed to create CSV file.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    /**
     * Sends a notification to all entrants in the currently visible tab.
     * Implements US 02.07.01 (Waiting), 02.07.02 (Invited), 02.07.03 (Cancelled).
     */
    private fun notifyAllInCurrentTab() {
        val statusFilters = when (currentTab) {
            "invited"   -> listOf("Pending", "Accepted")
            "cancelled" -> listOf("Declined", "Cancelled")
            else        -> null // waiting uses a different collection
        }

        db.collection("events").document(eventId).get().addOnSuccessListener { eventDoc ->
            val eventName = eventDoc.getString("name") ?: "Event"

            if (currentTab == "waiting") {
                db.collection("events").document(eventId).collection("waitlist").get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            Toast.makeText(requireContext(), "Waiting list is empty.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        snapshot.documents.forEach { doc ->
                            NotificationManager.sendNotification(
                                doc.id, "Organizer Update",
                                "New update for $eventName: Check your status!",
                                "ORGANIZER", eventName, eventId
                            )
                        }
                        Toast.makeText(requireContext(), "Notifications sent to waiting list.", Toast.LENGTH_SHORT).show()
                    }
            } else if (statusFilters != null) {
                db.collection("events").document(eventId).collection("selected_list")
                    .whereIn("Status", statusFilters).get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            Toast.makeText(requireContext(), "List is empty.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        snapshot.documents.forEach { doc ->
                            NotificationManager.sendNotification(
                                doc.id, "Organizer Update",
                                "New update for $eventName: Check your status!",
                                "ORGANIZER", eventName, eventId
                            )
                        }
                        Toast.makeText(requireContext(), "Notifications sent.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    /**
     * Shows a dialog for the organizer to invite a specific entrant to a private event.
     * Implements US 01.05.06.
     */
    private fun showInviteDialog() {
        val input = EditText(requireContext())
        input.hint = "Enter Device ID"

        AlertDialog.Builder(requireContext())
            .setTitle("Private Event Invitation")
            .setMessage("Enter the Device ID of the person you want to invite.")
            .setView(input)
            .setPositiveButton("Send Invite") { _, _ ->
                val deviceId = input.text.toString().trim()
                if (deviceId.isNotEmpty()) {
                    db.collection("events").document(eventId).get().addOnSuccessListener { doc ->
                        val eventName = doc.getString("name") ?: "Private Event"
                        NotificationManager.sendNotification(
                            deviceId, "Private Invitation",
                            "You have been invited to join the waiting list for $eventName!",
                            "ORGANIZER", eventName, eventId
                        )
                        Toast.makeText(requireContext(), "Invite sent!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves a list of device IDs to display names by looking up each user in Firestore.
     * Falls back to the device ID itself if the user document doesn't exist.
     *
     * @param deviceIds List of device IDs to resolve
     * @param onComplete Callback returning the resolved list of names in the same order
     */
    private fun resolveNames(deviceIds: List<String>, onComplete: (List<String>) -> Unit) {
        if (deviceIds.isEmpty()) {
            onComplete(emptyList())
            return
        }
        val names = MutableList(deviceIds.size) { deviceIds[it] }
        var resolved = 0
        for ((index, deviceId) in deviceIds.withIndex()) {
            db.collection("users").document(deviceId).get()
                .addOnSuccessListener { userDoc ->
                    if (userDoc.exists()) names[index] = userDoc.getString("name") ?: deviceId
                    resolved++
                    if (resolved == deviceIds.size) onComplete(names)
                }
                .addOnFailureListener {
                    resolved++
                    if (resolved == deviceIds.size) onComplete(names)
                }
        }
    }
}
