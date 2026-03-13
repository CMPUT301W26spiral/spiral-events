package com.example.spiral_event_lottery_app.ui.organizer_view

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.ContextCompat

/**
 * ManageEntrantsFragment displays the list of entrants for a specific event.
 *
 * Accessed by the organizer via the "View Entrants" button on the Event Details screen.
 *
 * Displays two active tabs:
 * - Invited: entrants who were selected by the lottery draw (from selected_list subcollection)
 * - Waiting: entrants still on the waiting list (from waitlist subcollection)
 *
 * User Stories implemented:
 * - US 02.02.01: View list of entrants who joined the event waiting list
 * - US 02.06.01: View list of chosen entrants who are invited to apply
 *
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

        // Placeholder clicks for now
        btnDrawReplacement.setOnClickListener { }
        btnExportCsv.setOnClickListener { }

        showTab("waiting")
        loadWaitingEntrants()
        loadInvitedEntrants()
    }

    private fun showTab(tab: String) {
        currentTab = tab
        invitedRecycler.visibility = if (tab == "invited") View.VISIBLE else View.GONE
        waitingRecycler.visibility = if (tab == "waiting") View.VISIBLE else View.GONE
        cancelledRecycler.visibility = if (tab == "cancelled") View.VISIBLE else View.GONE

        // Update tab button colors
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

    /**
     * Loads waiting entrants from waitlist subcollection.
     * Looks up each user's name from the users collection using device_id.
     */
    private fun loadWaitingEntrants() {
        db.collection("events")
            .document(eventId)
            .collection("waitlist")
            .get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.getString("device_id") ?: it.id }

                waitingCountText.text = "${deviceIds.size} People on Waiting List"

                if (deviceIds.isEmpty()) {
                    waitingRecycler.adapter = EntrantAdapter(emptyList())
                    return@addOnSuccessListener
                }

                // Look up names from users collection
                resolveNames(deviceIds) { names ->
                    waitingRecycler.adapter = EntrantAdapter(names)
                }
            }
    }

    /**
     * Loads invited entrants from selected_list subcollection.
     * Looks up each user's name from the users collection using device_id.
     */
    private fun loadInvitedEntrants() {
        db.collection("events")
            .document(eventId)
            .collection("selected_list")
            .get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.getString("device_id") ?: it.id }

                if (deviceIds.isEmpty()) {
                    invitedRecycler.adapter = EntrantAdapter(emptyList())
                    return@addOnSuccessListener
                }

                resolveNames(deviceIds) { names ->
                    invitedRecycler.adapter = EntrantAdapter(names)
                }
            }
    }

    /**
     * Looks up user names from the users collection given a list of device IDs.
     * Falls back to the device ID if no user document is found.
     *
     * @param deviceIds list of device IDs to look up
     * @param onComplete callback with list of resolved display names
     */
    private fun resolveNames(deviceIds: List<String>, onComplete: (List<String>) -> Unit) {
        val names = MutableList(deviceIds.size) { deviceIds[it] }
        var resolved = 0

        for ((index, deviceId) in deviceIds.withIndex()) {
            db.collection("users")
                .document(deviceId)
                .get()
                .addOnSuccessListener { userDoc ->
                    if (userDoc.exists()) {
                        names[index] = userDoc.getString("name") ?: deviceId
                    }
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