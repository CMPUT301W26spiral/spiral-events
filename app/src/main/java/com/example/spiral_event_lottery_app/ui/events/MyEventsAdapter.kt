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

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_my_event, parent, false)
        return VH(v)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * @param holder The ViewHolder which should be updated to represent the contents of the item.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(events[position], onDetails)

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = events.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.eventTitle)
        private val time = itemView.findViewById<TextView>(R.id.eventTime)
        private val location = itemView.findViewById<TextView>(R.id.eventLocation)
        private val waiting = itemView.findViewById<TextView>(R.id.eventWaiting)
        private val details = itemView.findViewById<Button>(R.id.detailsButton)
        private val poster = itemView.findViewById<ImageView>(R.id.eventPoster)

        fun bind(event: Event, onDetails: (Event) -> Unit) {
            val builder = SpannableStringBuilder(event.name)
            if (!event.isPublic) {
                val start = builder.length
                builder.append(" (Private)")
                // Make the "(Private)" text gray and slightly smaller for a professional look
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
