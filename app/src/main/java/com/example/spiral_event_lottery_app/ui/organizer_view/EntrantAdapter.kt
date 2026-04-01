package com.example.spiral_event_lottery_app.ui.organizer_view

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R

/**
 * EntrantAdapter displays a list of entrants in the Manage Entrants screen.
 *
 * Each item shows the entrant's name and an optional status badge
 * (Pending, Accepted, Declined, Cancelled). A remove button is shown
 * only when an onRemove callback is provided (i.e. on the Invited tab).
 *
 * Used by: ManageEntrantsFragment
 *
 * User Stories:
 * - US 02.02.01: View list of entrants on waiting list
 * - US 02.06.01: View list of chosen entrants
 * - US 02.06.02: View cancelled entrants
 * - US 02.06.03: View final enrolled entrants (Accepted status)
 * - US 02.06.04: Cancel entrants via remove button
 *
 * @param entrants List of Pair(displayName, status). Status is empty string for Waiting tab.
 * @param onRemove Optional callback when the remove button is tapped. Receives the display name.
 */
class EntrantAdapter(
    private var entrants: List<Pair<String, String>>,
    private val onRemove: ((String) -> Unit)? = null
) : RecyclerView.Adapter<EntrantAdapter.VH>() {

    /**
     * Updates the entrant list and refreshes the RecyclerView.
     *
     * @param newList New list of Pair(displayName, status)
     */
    fun submitList(newList: List<Pair<String, String>>) {
        entrants = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entrant, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (name, status) = entrants[position]
        holder.bind(name, status, onRemove)
    }

    override fun getItemCount(): Int = entrants.size

    /**
     * ViewHolder for a single entrant row.
     * Handles name display, status badge color, and remove button visibility.
     */
    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val nameText: TextView = itemView.findViewById(R.id.entrantName)
        private val statusBadge: TextView = itemView.findViewById(R.id.statusBadge)
        private val removeBtn: ImageButton = itemView.findViewById(R.id.removeButton)

        /**
         * Binds entrant data to the row.
         *
         * @param name        The entrant's display name or device ID
         * @param status      The entrant's current status string (empty = no badge shown)
         * @param onRemove    Optional callback for the remove button
         */
        fun bind(name: String, status: String, onRemove: ((String) -> Unit)?) {
            nameText.text = name

            // Show status badge only for Invited and Cancelled tabs
            if (status.isNotEmpty()) {
                statusBadge.visibility = View.VISIBLE
                statusBadge.text = status

                // Color the badge based on status
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

            // Remove button only shown on Invited tab (when callback provided)
            if (onRemove != null) {
                removeBtn.visibility = View.VISIBLE
                removeBtn.setOnClickListener { onRemove(name) }
            } else {
                removeBtn.visibility = View.GONE
            }
        }
    }
}