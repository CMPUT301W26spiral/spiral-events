package com.example.spiral_event_lottery_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.model.Event

/**
 * Recycler view adapter used to display the list of events that entrants can view and join.
 * Updated to handle different button text and actions based on whether the user is the organizer.
 */
class EventAdapter(
    private var allEvents: List<Event>,
    private val deviceId: String,
    private val onDetailsClicked: (Event) -> Unit,
    private val onSignUpClicked: (Event) -> Unit,
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private var filteredEvents: List<Event> = allEvents

    fun submitList(newList: List<Event>) {
        allEvents = newList
        filteredEvents = newList
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredEvents = if (query.isEmpty()) {
            allEvents
        } else {
            allEvents.filter { event ->
                event.name.contains(query, ignoreCase = true) ||
                        event.locationName.contains(query, ignoreCase = true) ||
                        event.description.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_card, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(filteredEvents[position], deviceId, onDetailsClicked, onSignUpClicked)
    }

    override fun getItemCount(): Int = filteredEvents.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val locationText: TextView = itemView.findViewById(R.id.locationText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val waitingText: TextView = itemView.findViewById(R.id.waitingText)
        private val actionButton: Button = itemView.findViewById(R.id.signUpButton)
        private val posterImage: ImageView = itemView.findViewById(R.id.eventPosterImage)

        fun bind(
            event: Event,
            deviceId: String,
            onDetailsClicked: (Event) -> Unit,
            onSignUpClicked: (Event) -> Unit
        ) {
            titleText.text = event.name
            locationText.text = event.locationName
            timeText.text = event.timeText
            waitingText.text = "${event.waitingCount} People on Waiting List"
            
            if (!event.posterUriString.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(event.posterUriString)
                    .placeholder(R.drawable.ic_event)
                    .into(posterImage)
            } else {
                posterImage.setImageResource(R.drawable.ic_event)
            }

            // Change button text and action based on organizer status
            if (event.organizerId == deviceId) {
                actionButton.text = "Details"
                actionButton.setOnClickListener { onDetailsClicked(event) }
                itemView.setOnClickListener { onDetailsClicked(event) }
            } else {
                actionButton.text = "Sign Up"
                actionButton.setOnClickListener { onSignUpClicked(event) }
                itemView.setOnClickListener { onSignUpClicked(event) }
            }
        }
    }
}
