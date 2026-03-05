package com.example.spiral_event_lottery_app.ui.events

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.EventStore
import com.example.spiral_event_lottery_app.ui.details.EventDetailsLeaveFragment

class MyEventsFragment : Fragment(R.layout.fragment_my_events) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyEventsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton?>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView = view.findViewById(R.id.currentEventsRecyclerView)

        adapter = MyEventsAdapter(EventStore.joinedEvents().toList()) { event ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EventDetailsLeaveFragment.newInstance(event.id))
                .addToBackStack(null)
                .commit()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        updateCounts(view)
    }

    override fun onResume() {
        super.onResume()
        // refresh list after join/leave
        adapter.submitList(EventStore.joinedEvents().toList())
        view?.let { updateCounts(it) }
    }

    private fun updateCounts(view: View) {
        val currentCountTv: TextView? = view.findViewById(R.id.currentCount)
        val organizerCountTv: TextView? = view.findViewById(R.id.organizerCount)

        val current = EventStore.joinedEvents()

        currentCountTv?.text = "${current.size} Event(s)"
        organizerCountTv?.text = "0 Event(s)"
    }
}