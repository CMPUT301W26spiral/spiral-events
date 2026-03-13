package com.example.spiral_event_lottery_app.ui.odetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spiral_event_lottery_app.R

/**
 * Adapter for displaying users selected in a lottery draw.
 */
class SelectedUsersAdapter(
    private var users: List<SelectedUser>
) : RecyclerView.Adapter<SelectedUsersAdapter.VH>() {

    /**
     * Updates the list of users and refreshes the UI.
     */
    fun submitList(newList: List<SelectedUser>) {
        users = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_user, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(users[position])
    override fun getItemCount(): Int = users.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText = itemView.findViewById<TextView>(R.id.userIdText) // Reusing ID for Name
        private val profileImageView = itemView.findViewById<ImageView>(R.id.profileImageView)

        fun bind(user: SelectedUser) {
            // Display the user's name instead of ID
            nameText.text = user.name

            // Load profile picture
            if (!user.profileUrl.isNullOrEmpty()) {
                Glide.with(profileImageView.context)
                    .load(user.profileUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(profileImageView)
            } else {
                profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }
}

/**
 * Model class for a user selected in a lottery draw.
 */
data class SelectedUser(
    val userId: String,
    val name: String,
    val profileUrl: String?,
    val selectedAt: Long?
)
