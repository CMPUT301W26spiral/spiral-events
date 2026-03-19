package com.example.spiral_event_lottery_app.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.ui.details.EventDetailsFragment
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.ui.oevent.EventDetailsOFragment
import com.google.firebase.firestore.ListenerRegistration

/**
 * HomeFragment displays a list of all open events.
 * It identifies if the current user is the organizer of an event to show different options.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventAdapter
    private lateinit var repository: EventRepository
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var searchEditText: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventRepository(requireContext())
        recyclerView = view.findViewById(R.id.eventsRecyclerView)
        searchEditText = view.findViewById(R.id.searchEditText)
        
        // Retrieve the current device ID to check against organizerId
        val deviceId = DeviceIdProvider.getDeviceId(requireContext())

        adapter = EventAdapter(
            allEvents = emptyList(),
            deviceId = deviceId,
            onDetailsClicked = { event ->
                // Navigate to Organizer Details if the user owns the event
                parentFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer,
                        EventDetailsOFragment.newInstance(event.id),
                        "details_screen")
                    .addToBackStack("details")
                    .commit()
            },
            onSignUpClicked = { event ->
                // Navigate to Entrant Details for signing up
                parentFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer,
                        EventDetailsFragment.newInstance(event.id),
                        "details_screen")
                    .addToBackStack("details")
                    .commit()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Set up search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 1. Find the Scan Button using the ID from your XML
        val scanBtn = view.findViewById<android.widget.Button>(R.id.scanButton)
        // 2. Set the click listener to open your camera
        scanBtn.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.example.spiral_event_lottery_app.QR_scanner::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        // Listen for real-time updates to the events collection
        listenerRegistration = repository.listenToOpenEvents(
            { events -> 
                adapter.submitList(events)
                // Re-apply filter if there's text in the search bar
                val currentSearch = searchEditText.text.toString()
                if (currentSearch.isNotEmpty()) {
                    adapter.filter(currentSearch)
                }
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
