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

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventAdapter
    private lateinit var repository: EventRepository
    private var listenerRegistration: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventRepository(requireContext())
        recyclerView = view.findViewById(R.id.eventsRecyclerView)

        adapter = EventAdapter(
            events = emptyList(),
            onItemClicked = { event ->
                parentFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, EventDetailsFragment.newInstance(event.id), "details_screen")
                    .addToBackStack("details")
                    .commit()
            },
            onSignUpClicked = { event ->
                // Sign up logic (for now ignore as requested, but keep the navigation if that's what was intended)
                // Actually user said "except the sign up button. for now ignore."
                // So sign up button click should probably do nothing or keep its current behavior.
                // Current behavior was navigating to details. I'll leave it or make it do nothing if "ignore" means that.
                // Usually "ignore" in this context might mean "don't change its behavior" or "don't make it go to details if it wasn't".
                // But the request says "anywhere you click... except the sign up button... it will send you to event details".
                // So I'll keep the sign up logic separate.
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        listenerRegistration = repository.listenToOpenEvents({ events -> adapter.submitList(events) }, { })
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
