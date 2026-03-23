package com.example.spiral_event_lottery_app.ui.organizer_view

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.ContextCompat

/**
 * ManageEntrantsFragment displays the list of entrants for a specific event.
 * Handles mass notifications and private invitations.
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

    private var currentTab = "waiting"

    companion object {
        fun newInstance(eventId: String): ManageEntrantsFragment {
            return ManageEntrantsFragment().apply {
                arguments = Bundle().apply { putString("event_id", eventId) }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        eventId = arguments?.getString("event_id") ?: ""

        // Bind new buttons
        btnInvitePrivate = view.findViewById(R.id.btnInvitePrivate)
        btnNotifyAll = view.findViewById(R.id.btnNotifyAll)
        
        btnInvited = view.findViewById(R.id.btnInvited)
        btnWaiting = view.findViewById(R.id.btnWaiting)
        btnCancelled = view.findViewById(R.id.btnCancelled)
        invitedRecycler = view.findViewById(R.id.invitedRecycler)
        waitingRecycler = view.findViewById(R.id.waitingRecycler)
        cancelledRecycler = view.findViewById(R.id.cancelledRecycler)
        waitingCountText = view.findViewById(R.id.waitingCountText)
        btnDrawReplacement = view.findViewById(R.id.btnDrawReplacement)
        btnExportCsv = view.findViewById(R.id.btnExportCsv)

        invitedRecycler.layoutManager = LinearLayoutManager(requireContext())
        waitingRecycler.layoutManager = LinearLayoutManager(requireContext())
        cancelledRecycler.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<View>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnInvited.setOnClickListener { showTab("invited") }
        btnWaiting.setOnClickListener { showTab("waiting") }
        btnCancelled.setOnClickListener { showTab("cancelled") }

        // US 02.07.01, 02.07.02, 02.07.03 logic
        btnNotifyAll.setOnClickListener {
            notifyAllInCurrentTab()
        }

        // US 01.05.06 logic
        btnInvitePrivate.setOnClickListener {
            showInviteDialog()
        }

        btnDrawReplacement.setOnClickListener { }
        btnExportCsv.setOnClickListener { }

        showTab("waiting")
        loadWaitingEntrants()
        loadInvitedEntrants()
        loadCancelledEntrants()
    }

    /**
     * Sends a notification to every entrant in the currently visible list.
     * Handles US 02.07.01 (Waiting), 02.07.02 (Selected), and 02.07.03 (Cancelled).
     */
    private fun notifyAllInCurrentTab() {
        val collectionPath = when (currentTab) {
            "invited" -> "selected_list"
            "waiting" -> "waitlist"
            "cancelled" -> "cancelled_list"
            else -> "waitlist"
        }

        db.collection("events").document(eventId).get().addOnSuccessListener { eventDoc ->
            val eventName = eventDoc.getString("name") ?: "Event"
            
            db.collection("events").document(eventId).collection(collectionPath).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        Toast.makeText(requireContext(), "List is empty.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    snapshot.documents.forEach { doc ->
                        NotificationManager.sendNotification(
                            doc.id,
                            "Organizer Update",
                            "New update for $eventName: Check your status!",
                            "ORGANIZER",
                            eventName,
                            eventId
                        )
                    }
                    Toast.makeText(requireContext(), "Notifications sent to all entrants in $currentTab list.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Opens a dialog to invite a specific entrant to a private event.
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
                            deviceId,
                            "Private Invitation",
                            "You have been invited to join the waiting list for $eventName!",
                            "ORGANIZER",
                            eventName,
                            eventId
                        )
                        Toast.makeText(requireContext(), "Invite sent!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTab(tab: String) {
        currentTab = tab
        invitedRecycler.visibility = if (tab == "invited") View.VISIBLE else View.GONE
        waitingRecycler.visibility = if (tab == "waiting") View.VISIBLE else View.GONE
        cancelledRecycler.visibility = if (tab == "cancelled") View.VISIBLE else View.GONE

        btnNotifyAll.text = "Notify All ${tab.replaceFirstChar { it.uppercase() }} Entrants"

        val activeColor = ContextCompat.getColor(requireContext(), R.color.primary_green)
        val inactiveColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val activeText = ContextCompat.getColor(requireContext(), android.R.color.white)
        val inactiveText = ContextCompat.getColor(requireContext(), R.color.primary_green)

        listOf(btnInvited, btnWaiting, btnCancelled).forEach { btn ->
            btn.setBackgroundColor(inactiveColor)
            btn.setTextColor(inactiveText)
        }

        val activeBtn = when (tab) {
            "invited" -> btnInvited
            "waiting" -> btnWaiting
            else -> btnCancelled
        }
        activeBtn.setBackgroundColor(activeColor)
        activeBtn.setTextColor(activeText)
    }

    private fun loadWaitingEntrants() {
        db.collection("events").document(eventId).collection("waitlist").get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.id }
                waitingCountText.text = "${deviceIds.size} People on Waiting List"
                resolveNames(deviceIds) { names -> waitingRecycler.adapter = EntrantAdapter(names) }
            }
    }

    private fun loadInvitedEntrants() {
        db.collection("events").document(eventId).collection("selected_list").get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.id }
                resolveNames(deviceIds) { names -> invitedRecycler.adapter = EntrantAdapter(names) }
            }
    }

    private fun loadCancelledEntrants() {
        db.collection("events").document(eventId).collection("cancelled_list").get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.id }
                resolveNames(deviceIds) { names -> cancelledRecycler.adapter = EntrantAdapter(names) }
            }
    }

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
