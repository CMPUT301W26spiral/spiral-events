package com.example.spiral_event_lottery_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.TagRepository
import com.example.spiral_event_lottery_app.model.Event
import com.example.spiral_event_lottery_app.model.User
import com.example.spiral_event_lottery_app.ui.events.PosterAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Recycler view adapter used to display the list of events that entrants can view and join.
 * Updated to handle multiple posters and smart recommendation.
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
    private var currentUser: User? = null
    private val tagRepository = TagRepository()

    fun submitList(newList: List<Event>) {
        allEvents = newList
        applyFilters()
    }

    fun setCurrentUser(user: User?) {
        this.currentUser = user
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
        
        val baseFiltered = allEvents.filter { event ->
            val matchesSearch = if (currentSearchQuery.isEmpty()) {
                true
            } else {
                event.name.contains(currentSearchQuery, ignoreCase = true) ||
                        event.locationName.contains(currentSearchQuery, ignoreCase = true) ||
                        event.description.contains(currentSearchQuery, ignoreCase = true) ||
                        event.interests.contains(currentSearchQuery, ignoreCase = true)
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

        filteredEvents = if (currentUser != null) {
            baseFiltered.sortedByDescending { calculateRelevanceScore(it) }
        } else {
            baseFiltered
        }

        notifyDataSetChanged()
    }

    private fun calculateRelevanceScore(event: Event): Int {
        val user = currentUser ?: return 0
        var score = 0
        
        val eventTags = event.interests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        for (tag in eventTags) {
            if (user.notInterested.contains(tag)) {
                score -= 20
                continue
            }

            if (user.interested.contains(tag)) {
                score += 10
            } else {
                val tagInfo = tagRepository.getTagImmediate(tag)
                for (parent in tagInfo.parents) {
                    if (user.interested.contains(parent)) {
                        score += 5
                        break
                    }
                }
            }
        }
        return score
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
        private val posterViewPager: ViewPager2 = itemView.findViewById(R.id.eventPosterViewPager)
        private val posterIndicator: TabLayout = itemView.findViewById(R.id.posterIndicator)
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
            
            val posters = event.posterUriStrings.ifEmpty {
                if (event.posterUriString != null) listOf(event.posterUriString!!) else emptyList()
            }
            
            if (posters.isNotEmpty()) {
                posterViewPager.adapter = PosterAdapter(posters)
                TabLayoutMediator(posterIndicator, posterViewPager) { _, _ -> }.attach()
                posterIndicator.visibility = if (posters.size > 1) View.VISIBLE else View.GONE
            } else {
                posterViewPager.adapter = PosterAdapter(listOf("")) // placeholder
                posterIndicator.visibility = View.GONE
            }

            val isFull = event.maxEntrants != null && event.waitingCount >= event.maxEntrants!!
            
            if (isFull) {
                statusPill.text = "Full"
                statusPill.setBackgroundResource(R.drawable.bg_full_pill)
                statusPill.setTextColor(0xFFB00020.toInt())
            } else {
                statusPill.text = "Open"
                statusPill.setBackgroundResource(R.drawable.bg_open_pill)
                statusPill.setTextColor(0xFF1F5E3B.toInt())
            }

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
