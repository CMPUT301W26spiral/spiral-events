package com.example.spiral_event_lottery_app.ui.events

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.ui.details.EventDetailsLeaveFragment

/**
 * Fragment that displays the list of events the current entrant has joined
 * Corresponds to the My Events page
 * Shows all events which the current entrant has joined and the events are retrieved through EventRepository
 * Core Functionality:
 * 1. Viewing all events the entrant has joined
 * 2. Navigating to the event details screen
 * 3. leaving the waiting list for an entrant
 */
class MyEventsFragment : Fragment(R.layout.fragment_my_events) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyEventsAdapter
    private lateinit var repository: EventRepository

    /**
     * Initalizes the UI and loads the joined events
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventRepository(requireContext())

        view.findViewById<ImageButton?>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView = view.findViewById(R.id.currentEventsRecyclerView)

        adapter = MyEventsAdapter(emptyList()) { event ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EventDetailsLeaveFragment.newInstance(event.id))
                .addToBackStack(null)
                .commit()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        refreshMyEvents()
    }

    /**
     * Refreshes the list of joined events when the user returns to the page
     */
    override fun onResume() {
        super.onResume()
        refreshMyEvents()
    }

    /**
     * Retrieves the list of events the entrant has joined and updates the RecyclerView
     */
    private fun refreshMyEvents() {
        repository.fetchMyEvents(
            { events ->
                adapter.submitList(events)
                view?.let { updateCounts(it, events.size) }
            },
            { }
        )
    }

    /**
     * Updates the event counters
     */
    private fun updateCounts(view: View, count: Int) {
        val currentCountTv: TextView? = view.findViewById(R.id.currentCount)
        val organizerCountTv: TextView? = view.findViewById(R.id.organizerCount)
        currentCountTv?.text = "$count Event(s)"
        organizerCountTv?.text = "0 Event(s)"
    }
}
