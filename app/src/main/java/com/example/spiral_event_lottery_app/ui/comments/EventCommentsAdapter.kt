package com.example.spiral_event_lottery_app.ui.comments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.model.EventComment
import java.text.SimpleDateFormat
import java.util.Locale

class EventCommentsAdapter(
    private val onDeleteClick: (EventComment) -> Unit
) : RecyclerView.Adapter<EventCommentsAdapter.CommentViewHolder>() {

    private val items = mutableListOf<EventComment>()
    private var canDelete = false

    fun submitList(comments: List<EventComment>) {
        items.clear()
        items.addAll(comments)
        notifyDataSetChanged()
    }

    fun setCanDelete(value: Boolean) {
        canDelete = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(items[position], canDelete, onDeleteClick)
    }

    override fun getItemCount(): Int = items.size

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val authorName: TextView = itemView.findViewById(R.id.commentAuthorName)
        private val role: TextView = itemView.findViewById(R.id.commentRole)
        private val body: TextView = itemView.findViewById(R.id.commentBody)
        private val time: TextView = itemView.findViewById(R.id.commentTime)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.commentDeleteButton)

        fun bind(
            comment: EventComment,
            canDelete: Boolean,
            onDeleteClick: (EventComment) -> Unit
        ) {
            authorName.text = comment.authorName
            role.text = comment.role
            body.text = comment.text

            val timestamp = comment.createdAt?.toDate()
            val formatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.CANADA)
            time.text = if (timestamp != null) formatter.format(timestamp) else ""

            deleteButton.visibility = if (canDelete) View.VISIBLE else View.GONE
            deleteButton.setOnClickListener {
                onDeleteClick(comment)
            }
        }
    }
}