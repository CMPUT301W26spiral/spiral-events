package com.example.spiral_event_lottery_app.ui.oevent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.model.User

/**
 * Adapter for displaying matching entrants in a search result list.
 * 
 * @property onUserClick Callback function triggered when a user item is selected.
 */
class UserSearchAdapter(private val onUserClick: (User) -> Unit) :
    RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {

    private var users: List<User> = emptyList()

    /**
     * Updates the data set and refreshes the RecyclerView.
     * @param newUsers The new list of users to display.
     */
    fun submitList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    /**
     * Called when RecyclerView needs a new [ViewHolder] to represent an item.
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entrant, parent, false)
        return ViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * @param holder The ViewHolder which should be updated to represent the contents of the item.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user, onUserClick)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = users.size

    /**
     * ViewHolder for user search results, responsible for binding user data to UI components.
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.entrantName)

        /**
         * Binds user data to the view and sets up the click listener.
         * @param user The user object to display.
         * @param onUserClick Callback for when the user item is clicked.
         */
        fun bind(user: User, onUserClick: (User) -> Unit) {
            nameText.text = "${user.name} (${user.email})"
            itemView.setOnClickListener { onUserClick(user) }
            
            // Search results are for invitation only, hide management-specific buttons
            itemView.findViewById<View>(R.id.removeButton)?.visibility = View.GONE
        }
    }
}
