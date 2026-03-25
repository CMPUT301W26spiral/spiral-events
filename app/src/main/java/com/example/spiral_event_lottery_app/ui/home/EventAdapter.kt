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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    enum class FilterStatus { ALL, OPEN, FULL }

    private var filteredEvents: List<Event> = allEvents
    private var currentSearchQuery: String = ""
    private var startDateFilter: Date? = null
    private var endDateFilter: Date? = null
    private var currentStatusFilter: FilterStatus = FilterStatus.ALL

    fun submitList(newList: List<Event>) {
        allEvents = newList
        applyFilters()
    }

    fun filter(query: String) {
        currentSearchQuery = query
        applyFilters()
    }

    fun setStatusFilter(status: FilterStatus) {
        currentStatusFilter = status
        applyFilters()
    }

    fun setDateRangeFilter(start: Date?, end: Date?) {
        startDateFilter = start
        endDateFilter = end
        applyFilters()
    }

    private fun applyFilters() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        filteredEvents = allEvents.filter { event ->
            val matchesSearch = if (currentSearchQuery.isEmpty()) {
                true
            } else {
                event.name.contains(currentSearchQuery, ignoreCase = true) ||
                        event.locationName.contains(currentSearchQuery, ignoreCase = true) ||
                        event.description.contains(currentSearchQuery, ignoreCase = true)
            }

            val matchesDateRange = if (startDateFilter == null || endDateFilter == null) {
                true
            } else {
                try {
                    val eventDate = dateFormat.parse(event.eventDate)
                    eventDate != null && !eventDate.before(startDateFilter) && !eventDate.after(endDateFilter)
                } catch (e: Exception) {
                    false
                }
            }

            val isFull = event.maxEntrants != null && event.waitingCount >= event.maxEntrants!!
            val matchesStatus = when (currentStatusFilter) {
                FilterStatus.ALL -> true
                FilterStatus.OPEN -> !isFull
                FilterStatus.FULL -> isFull
            }

            matchesSearch && matchesDateRange && matchesStatus
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
        private val statusPill: TextView = itemView.findViewById(R.id.statusPill)

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

            val isFull = event.maxEntrants != null && event.waitingCount >= event.maxEntrants!!
            
            // Update Status Pill
            if (isFull) {
                statusPill.text = "Full"
                statusPill.setBackgroundResource(R.drawable.bg_full_pill) // Assuming this exists or falls back
                statusPill.setTextColor(0xFFB00020.toInt()) // Red-ish
            } else {
                statusPill.text = "Open"
                statusPill.setBackgroundResource(R.drawable.bg_open_pill)
                statusPill.setTextColor(0xFF1F5E3B.toInt()) // Green-ish
            }

            // Change button text and action based on organizer status
            if (event.organizerId == deviceId) {
                actionButton.text = "Details"
                actionButton.isEnabled = true
                actionButton.setOnClickListener { onDetailsClicked(event) }
                itemView.setOnClickListener { onDetailsClicked(event) }
            } else {
                if (isFull) {
                    actionButton.text = "Full"
                    actionButton.isEnabled = false
                    actionButton.setOnClickListener(null)
                    // Even if full, we still allow clicking the item to view details
                    // This ensures the correct event is always opened
                    itemView.setOnClickListener { onSignUpClicked(event) }
                } else {
                    actionButton.text = "Sign Up"
                    actionButton.isEnabled = true
                    actionButton.setOnClickListener { onSignUpClicked(event) }
                    itemView.setOnClickListener { onSignUpClicked(event) }
                }
            }
        }
    }
}
