package com.example.spiral_event_lottery_app.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.EventStore
import com.example.spiral_event_lottery_app.ui.details.EventDetailsFragment

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.eventsRecyclerView)

        adapter = EventAdapter(EventStore.allEvents.toList()) { event ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EventDetailsFragment.newInstance(event.id))
                .addToBackStack(null)
                .commit()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // Refresh list so waitingCount changes
        adapter.submitList(EventStore.allEvents.toList())
    }
}