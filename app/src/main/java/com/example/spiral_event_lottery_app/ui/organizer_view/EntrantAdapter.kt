package com.example.spiral_event_lottery_app.ui.organizer_view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R

/**
 * EntrantAdapter is a RecyclerView adapter that displays a list of entrants
 * for a specific tab (Invited, Waiting, or Cancelled) in the ManageEntrantsFragment.
 *
 * Each item in the list displays the entrant's device ID and an optional
 * remove button for the organizer to cancel an entrant.
 *
 * Used by: ManageEntrantsFragment
 *
 *
 * @param entrants List of entrant device IDs to display.
 */
class EntrantAdapter(
    private var entrants: List<String>,
    private val onRemove: ((String) -> Unit)? = null
) : RecyclerView.Adapter<EntrantAdapter.VH>() {

    /**
     * Updates the list of entrants displayed by the adapter and refreshes the UI.
     *
     * @param newList The new list of entrant device IDs to display
     */
    fun submitList(newList: List<String>) {
        entrants = newList
        notifyDataSetChanged()
    }

    /**
     * Inflates the item layout and creates a new ViewHolder.
     *
     * @param parent The parent ViewGroup
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
     * @param holder The ViewHolder to bind
     * @param position The position in the list
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(entrants[position], onRemove)
    }

    /**
     * Returns the total number of entrants in the list.
     *
     * @return Size of the entrants list
     */
    override fun getItemCount(): Int = entrants.size

    /**
     * ViewHolder that holds references to the views for a single entrant item.
     * Displays the entrant's name/ID and a remove button if a callback is provided.
     *
     * @param itemView The inflated item view
     */
    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        /** TextView displaying the entrant's device ID or name */
        private val nameText: TextView = itemView.findViewById(R.id.entrantName)

        /** Button allowing the organizer to remove/cancel this entrant */
        private val removeBtn: ImageButton = itemView.findViewById(R.id.removeButton)

        /**
         * Binds an entrant's data to this ViewHolder.
         *
         * @param deviceId The entrant's device ID to display
         * @param onRemove Callback for when the remove button is clicked, or null to hide the button
         */
        fun bind(deviceId: String, onRemove: ((String) -> Unit)?) {
            // Display the entrant identifier
            nameText.text = deviceId

            // Show remove button only if organizer has remove capability for this tab
            if (onRemove != null) {
                removeBtn.visibility = View.VISIBLE
                removeBtn.setOnClickListener { onRemove(deviceId) }
            } else {
                removeBtn.visibility = View.GONE
            }
        }
    }
}