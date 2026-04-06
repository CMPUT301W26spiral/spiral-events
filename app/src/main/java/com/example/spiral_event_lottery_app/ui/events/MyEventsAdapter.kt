package com.example.spiral_event_lottery_app.ui.events

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.model.Event
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter used to display the list of events the current entrant has joined.
 * Updated to support multiple posters with ViewPager2.
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
        private val statusChip = itemView.findViewById<TextView>(R.id.statusChip)
        private val posterViewPager: ViewPager2 = itemView.findViewById(R.id.eventPosterViewPager)
        private val posterIndicator: TabLayout = itemView.findViewById(R.id.posterIndicator)

        fun bind(event: Event, onDetails: (Event) -> Unit) {
            val builder = SpannableStringBuilder(event.name)
            if (!event.isPublic) {
                val start = builder.length
                builder.append(" (Private)")
                builder.setSpan(
                    ForegroundColorSpan(Color.GRAY),
                    start,
                    builder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    RelativeSizeSpan(0.8f),
                    start,
                    builder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            title.text = builder
            
            time.text = event.timeText
            location.text = event.locationName
            waiting.text = "${event.waitingCount} People on Waiting List"
            
            if (event.lotteryDone) {
                statusChip.text = "Draw Done"
            } else {
                statusChip.text = getTimeRemainingText(event.drawDate, event.drawStartTime)
            }
            
            // Handle multiple posters
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

            details.setOnClickListener { onDetails(event) }
        }

        private fun getTimeRemainingText(drawDate: String, drawTime: String): String {
            val dateStr = drawDate.trim()
            val timeStr = drawTime.trim()
            
            if (dateStr.isEmpty() || timeStr.isEmpty()) return "N/A"
            
            try {
                val sdf = SimpleDateFormat("d/M/yyyy H:m", Locale.US)
                val targetDate = sdf.parse("$dateStr $timeStr") ?: return "N/A"
                
                val now = Date()
                val diffMillis = targetDate.time - now.time
                if (diffMillis <= 0) return "Draw Ended"

                val calTarget = Calendar.getInstance().apply { time = targetDate }
                val calNow = Calendar.getInstance().apply { time = now }
                
                var years = calTarget.get(Calendar.YEAR) - calNow.get(Calendar.YEAR)
                var months = calTarget.get(Calendar.MONTH) - calNow.get(Calendar.MONTH)
                
                if (calTarget.get(Calendar.DAY_OF_MONTH) < calNow.get(Calendar.DAY_OF_MONTH)) {
                    months--
                }
                
                if (months < 0) {
                    years--
                    months += 12
                }

                return when {
                    years > 0 -> "$years ${if (years == 1) "Year" else "Years"}"
                    months > 0 -> "$months ${if (months == 1) "Month" else "Months"}"
                    else -> {
                        val days = diffMillis / (1000 * 60 * 60 * 24)
                        "$days ${if (days == 1L) "Day" else "Days"}"
                    }
                }
            } catch (e: Exception) {
                return "N/A"
            }
        }
    }
}