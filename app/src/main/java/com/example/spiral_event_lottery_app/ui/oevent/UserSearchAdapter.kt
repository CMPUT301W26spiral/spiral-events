package com.example.spiral_event_lottery_app.ui.oevent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.model.User

class UserSearchAdapter(private val onUserClick: (User) -> Unit) :
    RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {

    private var users: List<User> = emptyList()

    fun submitList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entrant, parent, false) // Using item_entrant for now, might need custom one if different
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user, onUserClick)
    }

    override fun getItemCount(): Int = users.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.entrantName)

        fun bind(user: User, onUserClick: (User) -> Unit) {
            nameText.text = "${user.name} (${user.email})"
            itemView.setOnClickListener { onUserClick(user) }
            // Hide remove button if it exists in item_entrant layout as this is a search result
            itemView.findViewById<View>(R.id.removeButton)?.visibility = View.GONE
        }
    }
}
