package com.example.spiral_event_lottery_app.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.ui.details.EventDetailsFragment
import com.google.firebase.firestore.ListenerRegistration

/**
 * Fragment that displays the list of available events
 * Represents the home page of the application
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventAdapter
    private lateinit var repository: EventRepository
    private var listenerRegistration: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventRepository(requireContext())

        recyclerView = view.findViewById(R.id.eventsRecyclerView)

        adapter = EventAdapter(emptyList()) { event ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EventDetailsFragment.newInstance(event.id))
                .addToBackStack(null)
                .commit()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()

        listenerRegistration = repository.listenToOpenEvents(
            { events ->
                adapter.submitList(events)
            },
            { }
        )
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
