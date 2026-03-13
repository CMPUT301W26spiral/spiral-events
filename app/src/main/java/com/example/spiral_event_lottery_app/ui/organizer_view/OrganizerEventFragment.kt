package com.example.spiral_event_lottery_app.ui.organizer_view

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.spiral_event_lottery_app.R
import com.google.firebase.firestore.FirebaseFirestore

/**
 * OrganizerEventFragment displays the details of an event from the organizer's perspective.
 *
 * This fragment corresponds to the "Organizer Event Details" screen in the Figma mockup.
 * It is accessed when an organizer taps "Details" on one of their events in MyEventsFragment.
 *
 * Displays:
 * - Event name, location, time, description
 * - Number of people on waiting list and open spots
 * - Buttons to manage the event: View Entrants, Notify Entrants, View Locations
 *
 * User Stories implemented:
 * - US 02.02.01: View list of entrants who joined the waiting list (via View Entrants button)
 * - US 02.06.01: View list of chosen entrants (via View Entrants button)
 * - US 02.03.01: Optionally limit the number of entrants on the waiting list
 *
 * Data Source: Firebase Firestore
 * Collection path: events/{eventId}
 *
 * @param eventId The Firestore document ID of the event being viewed
 */
class OrganizerEventFragment : Fragment(R.layout.fragment_organizer_event) {

    /** The Firestore event ID passed in when this fragment is created */
    private lateinit var eventId: String

    /** Firestore database instance used to fetch event data */
    private val db = FirebaseFirestore.getInstance()

    companion object {
        /**
         * Creates a new instance of OrganizerEventFragment with the given event ID.
         * This is the recommended way to instantiate this fragment.
         *
         * @param eventId The Firestore document ID of the event to display
         * @return A new OrganizerEventFragment with eventId stored in arguments
         */
        fun newInstance(eventId: String): OrganizerEventFragment {
            return OrganizerEventFragment().apply {
                arguments = Bundle().apply { putString("event_id", eventId) }
            }
        }
    }

    /**
     * Called after the fragment's view has been created.
     * Loads event data from Firestore and sets up button click listeners.
     *
     * @param view The root view of the fragment layout
     * @param savedInstanceState Previously saved state, if any
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the event ID passed to this fragment
        eventId = arguments?.getString("event_id") ?: ""

        // Back button navigates to the previous screen
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // "View Entrants" button navigates to ManageEntrantsFragment
        // This covers US 02.02.01 and US 02.06.01
        view.findViewById<Button>(R.id.btnViewEntrants).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ManageEntrantsFragment.newInstance(eventId))
                .addToBackStack(null)
                .commit()
        }

        // "Notify Entrants" button - placeholder for future notification feature
        view.findViewById<Button>(R.id.btnNotifyEntrants).setOnClickListener {
            // TODO: wire up to notification feature when teammate completes it
        }

        // "View Locations" button - placeholder for future geolocation feature
        view.findViewById<Button>(R.id.btnViewLocations).setOnClickListener {
            // TODO: wire up to geolocation feature when teammate completes it
        }

        // Load event details from Firestore
        loadEventDetails(view)
    }

    /**
     * Fetches event details from Firestore and populates the UI views.
     * Reads the event document from the events collection using the eventId.
     *
     * Fields read from Firestore:
     * - name: event title
     * - location_name: where the event takes place
     * - description: event description text
     * - waiting_count: number of people on the waiting list
     * - max_entrants: optional cap on waiting list size (US 02.03.01)
     *
     * @param view The fragment's root view used to find UI elements
     */
    private fun loadEventDetails(view: View) {
        db.collection("events")
            .document(eventId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc == null || !doc.exists()) return@addOnSuccessListener

                // Populate event name
                val name = doc.getString("name") ?: "Unknown Event"
                view.findViewById<TextView>(R.id.organizerEventTitle).text = name

                // Populate location
                val location = doc.getString("location_name") ?: ""
                view.findViewById<TextView>(R.id.organizerEventLocation).text = location

                // Populate description
                val description = doc.getString("description") ?: ""
                view.findViewById<TextView>(R.id.organizerEventDescription).text = description

                // Populate waiting count and open spots
                val waitingCount = doc.getLong("waiting_count") ?: 0L
                val maxEntrants = doc.getLong("max_entrants") ?: 0L
                val openSpots = if (maxEntrants > 0) maxEntrants - waitingCount else 0L

                view.findViewById<TextView>(R.id.organizerEventWaiting).text =
                    "$waitingCount People on Waiting List" +
                            if (maxEntrants > 0) ", $openSpots Open Spots" else ""
            }
    }
}