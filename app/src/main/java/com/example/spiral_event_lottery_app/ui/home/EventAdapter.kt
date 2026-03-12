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
 * Recycler view adapter used to display the list of events that entrants can view and join
 * Binds the model data to the UI layout
 * Also provides a callback that allows the UI to respond when the user pressed Sign Up button
 */
class EventAdapter(
    private var events: List<Event>,
    private val onSignUpClicked: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    fun submitList(newList: List<Event>) {
        events = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_card, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position], onSignUpClicked)
    }

    override fun getItemCount(): Int = events.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val locationText: TextView = itemView.findViewById(R.id.locationText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val waitingText: TextView = itemView.findViewById(R.id.waitingText)
        private val signUpButton: Button = itemView.findViewById(R.id.signUpButton)
        private val posterImage: ImageView = itemView.findViewById(R.id.eventPosterImage)

        fun bind(event: Event, onSignUpClicked: (Event) -> Unit) {
            titleText.text = event.name
            locationText.text = event.locationName
            timeText.text = event.timeText
            waitingText.text = "${event.waitingCount} People on Waiting List"
            
            if (!event.posterUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(event.posterUrl)
                    .placeholder(R.drawable.ic_event)
                    .into(posterImage)
            } else {
                posterImage.setImageResource(R.drawable.ic_event)
            }

            signUpButton.setOnClickListener { onSignUpClicked(event) }
        }
    }
}