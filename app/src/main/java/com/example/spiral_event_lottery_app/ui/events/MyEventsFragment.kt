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
 */
class MyEventsFragment : Fragment(R.layout.fragment_my_events) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyEventsAdapter
    private lateinit var repository: EventRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventRepository(requireContext())

        view.findViewById<ImageButton?>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView = view.findViewById(R.id.currentEventsRecyclerView)

        adapter = MyEventsAdapter(emptyList()) { event ->
            // FIXED: Use .add() and "details_screen" tag to match MainActivity logic
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, EventDetailsLeaveFragment.newInstance(event.id), "details_screen")
                .addToBackStack("details")
                .commit()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        refreshMyEvents()
    }

    override fun onResume() {
        super.onResume()
        refreshMyEvents()
    }

    private fun refreshMyEvents() {
        repository.fetchMyEvents(
            { events ->
                adapter.submitList(events)
                view?.let { updateCounts(it, events.size) }
            },
            { }
        )
    }

    private fun updateCounts(view: View, count: Int) {
        val currentCountTv: TextView? = view.findViewById(R.id.currentCount)
        val organizerCountTv: TextView? = view.findViewById(R.id.organizerCount)
        currentCountTv?.text = "$count Event(s)"
        organizerCountTv?.text = "0 Event(s)"
    }
}
