package com.example.spiral_event_lottery_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.TagRepository
import com.example.spiral_event_lottery_app.model.Event
import com.example.spiral_event_lottery_app.model.User
import com.example.spiral_event_lottery_app.ui.events.PosterAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Recycler view adapter used to display the list of events that entrants can view and join.
 * Updated to handle multiple posters and smart recommendation with bidirectional parent tag support.
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
    private val adapterScope = CoroutineScope(Dispatchers.Main)

    fun submitList(newList: List<Event>) {
        allEvents = newList
        fetchTagsAndRefresh()
    }

    fun setCurrentUser(user: User?) {
        this.currentUser = user
        fetchTagsAndRefresh()
    }

    private fun fetchTagsAndRefresh() {
        val allTagIds = allEvents.flatMap { it.interests.split(",").map { s -> s.trim() } }
            .filter { it.isNotEmpty() }
            .toMutableList()
        
        currentUser?.let { user ->
            allTagIds.addAll(user.interested)
            allTagIds.addAll(user.notInterested)
        }
        
        val distinctTagIds = allTagIds.distinct()
        
        adapterScope.launch {
            withContext(Dispatchers.IO) {
                tagRepository.fetchAndCacheTags(distinctTagIds)
            }
            applyFilters()
        }
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
            baseFiltered.sortedWith(compareByDescending<Event> { calculateRelevanceScore(it) }
                .thenByDescending { it.eventCreated })
        } else {
            baseFiltered.sortedByDescending { it.eventCreated }
        }

        notifyDataSetChanged()
    }

    /**
     * Calculates a points-based relevance score for an event based on user interests.
     * Considers both direct tag matches and parent category matches in both directions.
     */
    private fun calculateRelevanceScore(event: Event): Int {
        val user = currentUser ?: return 0
        var score = 0
        
        val eventTags = event.interests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        for (tagName in eventTags) {
            val tagInfo = tagRepository.getTagImmediate(tagName)
            
            // 1. Direct Negative Match (Highest penalty)
            if (user.notInterested.contains(tagName)) {
                score -= 100
                continue
            }

            // 2. Direct Positive Match
            if (user.interested.contains(tagName)) {
                score += 50
            }

            // 3. Event Tag's Parents: Does the user like the general category this tag belongs to?
            // e.g. Event is "Concert", User likes "Music"
            for (parent in tagInfo.parents) {
                if (user.notInterested.contains(parent)) {
                    score -= 30
                }
                if (user.interested.contains(parent)) {
                    score += 25
                }
            }

            // 4. User's Interests' Parents: Is the event tagged with a category the user is interested in?
            // e.g. User likes "Band", Event is tagged "Music"
            for (userInterest in user.interested) {
                val userInterestTag = tagRepository.getTagImmediate(userInterest)
                if (userInterestTag.parents.contains(tagName)) {
                    score += 20 // The event tag is a parent of something the user specifically likes
                }
            }
        }
        
        // Boost events created by the user slightly
        if (event.organizerId == deviceId) {
            score += 10
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
