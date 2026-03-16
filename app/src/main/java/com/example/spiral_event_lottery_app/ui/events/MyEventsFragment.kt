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
import com.example.spiral_event_lottery_app.data.EventStoreO
import com.example.spiral_event_lottery_app.ui.details.EventDetailsLeaveFragment
import com.example.spiral_event_lottery_app.ui.oevent.EventDetailsOFragment

/**
 * Fragment that displays both joined events (Entrant) and organized events (Organizer).
 */
class MyEventsFragment : Fragment(R.layout.fragment_my_events) {

    private lateinit var joinedAdapter: MyEventsAdapter
    private lateinit var organizerAdapter: MyEventsAdapter
    
    private lateinit var repository: EventRepository
    private lateinit var eventStoreO: EventStoreO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventRepository(requireContext())
        eventStoreO = EventStoreO(requireContext())

        view.findViewById<ImageButton?>(R.id.backButtonMyEvents)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 1. Setup Joined Events RecyclerView
        val joinedRv = view.findViewById<RecyclerView>(R.id.currentEventsRecyclerView)
        joinedAdapter = MyEventsAdapter(emptyList()) { event ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EventDetailsLeaveFragment.newInstance(event.id))
                .addToBackStack(null)
                .commit()
        }
        joinedRv.layoutManager = LinearLayoutManager(requireContext())
        joinedRv.adapter = joinedAdapter

        // 2. Setup Organizer Events RecyclerView
        val organizerRv = view.findViewById<RecyclerView>(R.id.organizerEventsRecyclerView)
        organizerAdapter = MyEventsAdapter(emptyList()) { event ->
            // Use instance method or specific Organizer Details fragment if available
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EventDetailsOFragment.newInstance(event.id))
                .addToBackStack(null)
                .commit()
        }
        organizerRv.layoutManager = LinearLayoutManager(requireContext())
        organizerRv.adapter = organizerAdapter

        refreshData()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    /**
     * Refreshes the lists of both joined and organized events.
     * Must be public so MainActivity can trigger a refresh.
     */
    fun refreshData() {
        // Fetch Joined Events (Entrant view)
        repository.fetchMyEvents(
            { events ->
                joinedAdapter.submitList(events)
                view?.let { updateJoinedCount(it, events.size) }
            },
            { }
        )

        // Fetch Organized Events (Organizer view)
        eventStoreO.organizerEvents { events ->
            if (isAdded) {
                // Sort events by creation time descending (most recent first)
                // Ensures that the newest created events appear at the top
                val sortedEvents = events.sortedByDescending { it.eventCreated }
                organizerAdapter.submitList(sortedEvents)
                view?.let { updateOrganizerCount(it, events.size) }
            }
        }
    }

    private fun updateJoinedCount(view: View, count: Int) {
        view.findViewById<TextView>(R.id.currentCount)?.text = "$count Event(s)"
    }

    private fun updateOrganizerCount(view: View, count: Int) {
        view.findViewById<TextView>(R.id.organizerCount)?.text = "$count Event(s)"
    }
}
