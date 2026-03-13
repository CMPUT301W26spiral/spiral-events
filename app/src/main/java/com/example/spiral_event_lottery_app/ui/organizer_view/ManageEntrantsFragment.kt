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

/**
 * ManageEntrantsFragment displays the list of entrants for a specific event
 * organized by their status in the lottery system.
 *
 * This fragment corresponds to the "Manage Entrants" screen in the Figma mockup.
 * It is accessed by the organizer from the Organizer Event Details screen.
 *
 * Displays three tabs:
 * - Invited: entrants who have been selected by the lottery draw
 * - Waiting: entrants who are on the waiting list pending selection
 * - Cancelled: entrants who declined or were removed
 *
 * User Stories implemented:
 * - US 02.02.01: As an organizer I want to view the list of entrants who joined my event waiting list
 * - US 02.06.01: As an organizer I want to view a list of all chosen entrants who are invited to apply
 * - US 02.06.02: As an organizer I want to see a list of all the cancelled entrants
 *
 * Data Source: Firebase Firestore
 * Collection path: events/{eventId}/waitlist
 * Each waitlist document contains:
 * - device_id: the entrant's device identifier
 * - joined_at: timestamp when they joined
 * - status: "waiting", "invited", or "cancelled" (defaults to "waiting" if missing)
 */
class ManageEntrantsFragment : Fragment(R.layout.fragment_manage_entrants) {

    /** The Firestore event ID passed in when this fragment is created */
    private lateinit var eventId: String

    /** Firestore database instance used to fetch waitlist data */
    private val db = FirebaseFirestore.getInstance()

    /** RecyclerView for displaying invited entrants */
    private lateinit var invitedRecycler: RecyclerView

    /** RecyclerView for displaying waiting entrants */
    private lateinit var waitingRecycler: RecyclerView

    /** RecyclerView for displaying cancelled entrants */
    private lateinit var cancelledRecycler: RecyclerView

    /** Tab button for the Invited list */
    private lateinit var btnInvited: Button

    /** Tab button for the Waiting list */
    private lateinit var btnWaiting: Button

    /** Tab button for the Cancelled list */
    private lateinit var btnCancelled: Button

    companion object {
        /**
         * Creates a new instance of ManageEntrantsFragment with the given event ID.
         * This is the recommended way to instantiate this fragment.
         *
         * @param eventId The Firestore document ID of the event whose entrants are being managed
         * @return A new ManageEntrantsFragment instance with the eventId stored in its arguments
         */
        fun newInstance(eventId: String): ManageEntrantsFragment {
            return ManageEntrantsFragment().apply {
                arguments = Bundle().apply { putString("event_id", eventId) }
            }
        }
    }

    /**
     * Called after the fragment's view has been created.
     * Sets up the tab buttons, RecyclerViews, back button, and loads entrant data from Firestore.
     *
     * @param view The root view of the fragment layout
     * @param savedInstanceState Previously saved state, if any
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the event ID passed to this fragment
        eventId = arguments?.getString("event_id") ?: ""

        // Bind tab buttons from the layout
        btnInvited = view.findViewById(R.id.btnInvited)
        btnWaiting = view.findViewById(R.id.btnWaiting)
        btnCancelled = view.findViewById(R.id.btnCancelled)

        // Bind RecyclerViews for each tab
        invitedRecycler = view.findViewById(R.id.invitedRecycler)
        waitingRecycler = view.findViewById(R.id.waitingRecycler)
        cancelledRecycler = view.findViewById(R.id.cancelledRecycler)

        // Set layout managers so RecyclerViews display as vertical lists
        invitedRecycler.layoutManager = LinearLayoutManager(requireContext())
        waitingRecycler.layoutManager = LinearLayoutManager(requireContext())
        cancelledRecycler.layoutManager = LinearLayoutManager(requireContext())

        // Back button navigates to the previous screen
        view.findViewById<View>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Show the Invited tab by default when the screen opens
        showTab("invited")

        // Tab button click listeners switch which RecyclerView is visible
        btnInvited.setOnClickListener { showTab("invited") }
        btnWaiting.setOnClickListener { showTab("waiting") }
        btnCancelled.setOnClickListener { showTab("cancelled") }

        // Fetch entrant data from Firestore
        loadEntrants()
    }

    /**
     * Shows the RecyclerView for the selected tab and hides the others.
     * Only one tab's list is visible at a time.
     *
     * @param tab The tab to show. Must be one of: "invited", "waiting", "cancelled"
     */
    private fun showTab(tab: String) {
        invitedRecycler.visibility = if (tab == "invited") View.VISIBLE else View.GONE
        waitingRecycler.visibility = if (tab == "waiting") View.VISIBLE else View.GONE
        cancelledRecycler.visibility = if (tab == "cancelled") View.VISIBLE else View.GONE
    }

    /**
     * Fetches all entrants from the Firestore waitlist subcollection for this event.
     * Sorts each entrant into the invited, waiting, or cancelled list based on their status field.
     * If no status field exists, the entrant is treated as "waiting" by default.
     *
     * Firestore path: events/{eventId}/waitlist
     *
     * After fetching, updates each RecyclerView with the appropriate list
     * and updates the waiting count text.
     */
    private fun loadEntrants() {
        db.collection("events")
            .document(eventId)
            .collection("waitlist")
            .get()
            .addOnSuccessListener { snapshot ->

                // Separate lists for each status category
                val invited = mutableListOf<String>()
                val waiting = mutableListOf<String>()
                val cancelled = mutableListOf<String>()

                // Loop through each waitlist document and sort by status
                for (doc in snapshot.documents) {
                    // Default to "waiting" if no status field exists yet
                    val status = doc.getString("status") ?: "waiting"
                    // Use device_id field if available, otherwise fall back to document ID
                    val deviceId = doc.getString("device_id") ?: doc.id

                    when (status) {
                        "invited" -> invited.add(deviceId)
                        "cancelled" -> cancelled.add(deviceId)
                        else -> waiting.add(deviceId) // covers "waiting" and any unknown status
                    }
                }

                // Set adapters with the sorted lists
                invitedRecycler.adapter = EntrantAdapter(invited)
                waitingRecycler.adapter = EntrantAdapter(waiting)
                cancelledRecycler.adapter = EntrantAdapter(cancelled)

                // Update the waiting count display
                view?.findViewById<TextView>(R.id.waitingCountText)?.text =
                    "${waiting.size} People on Waiting List"
            }
    }
}