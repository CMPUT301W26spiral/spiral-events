package com.example.spiral_event_lottery_app.ui.organizer_view

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.model.User

/**
 * EntrantAdapter is a RecyclerView adapter that displays a list of entrants
 * for a specific tab (Invited, Waiting, or Cancelled) in the ManageEntrantsFragment.
 *
 * Each item displays the entrant's name, an optional status badge
 * (pending, accepted, declined, cancelled), a remove button for the organizer
 * to cancel an entrant, and a co-organizer assignment button.
 *
 * Used by: ManageEntrantsFragment
 *
 * User Stories:
 * - US 02.02.01: View list of entrants on waiting list
 * - US 02.06.01: View list of chosen entrants
 * - US 02.06.02: View cancelled entrants
 * - US 02.06.03: View final enrolled entrants (accepted status)
 * - US 02.06.04: Cancel entrants via remove button
 * - US 02.09.01: Assign entrant as co-organizer
 *
 * @param entrants            List of entrant User objects to display
 * @param statusMap           Optional map of deviceId to status string for badge display
 * @param onRemove            Optional callback when the remove button is tapped
 * @param onAssignCoOrganizer Optional callback when the co-organizer button is tapped
 */
class EntrantAdapter(
    private var entrants: List<User>,
    private val statusMap: Map<String, String> = emptyMap(),
    private val onRemove: ((User) -> Unit)? = null,
    private val onAssignCoOrganizer: ((User) -> Unit)? = null
) : RecyclerView.Adapter<EntrantAdapter.VH>() {

    /**
     * Updates the list of entrants displayed by the adapter and refreshes the UI.
     *
     * @param newList The new list of entrant User objects to display
     */
    fun submitList(newList: List<User>) {
        entrants = newList
        notifyDataSetChanged()
    }

    /**
     * Inflates the item layout and creates a new ViewHolder.
     *
     * @param parent   The parent ViewGroup
     * @param viewType The view type (not used here, single type)
     * @return A new VH instance
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entrant, parent, false)
        return VH(v)
    }

    /**
     * Binds data to the ViewHolder at the given position.
     *
     * @param holder   The ViewHolder to bind
     * @param position The position in the list
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = entrants[position]
        val status = statusMap[user.deviceId] ?: ""
        holder.bind(user, status, onRemove, onAssignCoOrganizer)
    }

    /**
     * Returns the total number of entrants in the list.
     *
     * @return Size of the entrants list
     */
    override fun getItemCount(): Int = entrants.size

    /**
     * ViewHolder that holds references to the views for a single entrant item.
     * Displays the entrant's name, status badge, remove button, and co-organizer button.
     *
     * @param itemView The inflated item view
     */
    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        /** TextView displaying the entrant's name or device ID */
        private val nameText: TextView = itemView.findViewById(R.id.entrantName)

        /** TextView badge showing the entrant's current status */
        private val statusBadge: TextView = itemView.findViewById(R.id.statusBadge)

        /** Button allowing the organizer to remove/cancel this entrant */
        private val removeBtn: ImageButton = itemView.findViewById(R.id.removeButton)

        /** Button allowing the organizer to assign this entrant as a co-organizer */
        private val coOrganizerBtn: Button = itemView.findViewById(R.id.makeCoOrganizerButton)

        /**
         * Binds an entrant's data to this ViewHolder.
         *
         * @param user                The entrant User object to display
         * @param status              The entrant's current status string (empty = no badge)
         * @param onRemove            Callback for the remove button, or null to hide it
         * @param onAssignCoOrganizer Callback for the co-organizer button, or null to hide it
         */
        fun bind(
            user: User,
            status: String,
            onRemove: ((User) -> Unit)?,
            onAssignCoOrganizer: ((User) -> Unit)?
        ) {
            // Display name or fall back to device ID
            nameText.text = if (user.name.isNotBlank()) user.name else user.deviceId

            // Show status badge only when a status is provided
            if (status.isNotEmpty()) {
                statusBadge.visibility = View.VISIBLE
                statusBadge.text = status.replaceFirstChar { it.uppercase() }

                // Color badge based on status
                val bgColor = when (status.lowercase()) {
                    "accepted"  -> Color.parseColor("#388E3C") // green
                    "pending"   -> Color.parseColor("#F57C00") // orange
                    "declined"  -> Color.parseColor("#C62828") // red
                    "cancelled" -> Color.parseColor("#C62828") // red
                    else        -> Color.parseColor("#757575") // grey fallback
                }
                statusBadge.background.setTint(bgColor)
            } else {
                statusBadge.visibility = View.GONE
            }

            // Show remove button only on Invited tab when callback is provided
            if (onRemove != null) {
                removeBtn.visibility = View.VISIBLE
                removeBtn.setOnClickListener { onRemove(user) }
            } else {
                removeBtn.visibility = View.GONE
            }

            // Show co-organizer button only on Waiting tab when callback is provided
            if (onAssignCoOrganizer != null) {
                coOrganizerBtn.visibility = View.VISIBLE
                coOrganizerBtn.setOnClickListener { onAssignCoOrganizer(user) }
                itemView.setOnClickListener { onAssignCoOrganizer(user) }
            } else {
                coOrganizerBtn.visibility = View.GONE
                itemView.setOnClickListener(null)
            }
        }
    }
}