package com.example.spiral_event_lottery_app.ui.events

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
 * RecyclerView adapter used to display the list of events the current entrant has joined.
 */
class MyEventsAdapter(
    private var events: List<Event>,
    private val onDetails: (Event) -> Unit
) : RecyclerView.Adapter<MyEventsAdapter.VH>() {

    /**
     * Updates the list of events displayed by the adapter and refreshes the UI.
     */
    fun submitList(newList: List<Event>) {
        events = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_my_event, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(events[position], onDetails)
    override fun getItemCount(): Int = events.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.eventTitle)
        private val time = itemView.findViewById<TextView>(R.id.eventTime)
        private val location = itemView.findViewById<TextView>(R.id.eventLocation)
        private val waiting = itemView.findViewById<TextView>(R.id.eventWaiting)
        private val details = itemView.findViewById<Button>(R.id.detailsButton)
        private val poster = itemView.findViewById<ImageView>(R.id.eventPoster)

        fun bind(event: Event, onDetails: (Event) -> Unit) {
            title.text = event.name
            time.text = event.timeText
            location.text = event.locationName
            waiting.text = "${event.waitingCount} People on Waiting List"
            
            // Load the event poster using Glide
            if (!event.posterUriString.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(event.posterUriString)
                    .placeholder(R.drawable.ic_event) // Show default while loading
                    .into(poster)
            } else {
                // Show a default placeholder if no poster exists
                poster.setImageResource(R.drawable.ic_event)
            }

            details.setOnClickListener { onDetails(event) }
        }
    }
}
