package com.example.spiral_event_lottery_app.ui.comments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.EventCommentRepository
import com.google.firebase.firestore.ListenerRegistration

class EventCommentsFragment : Fragment() {

    companion object {
        private const val ARG_EVENT_ID = "event_id"
        private const val ARG_ORGANIZER_MODE = "organizer_mode"

        fun newInstance(eventId: String, organizerMode: Boolean): EventCommentsFragment {
            return EventCommentsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EVENT_ID, eventId)
                    putBoolean(ARG_ORGANIZER_MODE, organizerMode)
                }
            }
        }
    }

    private lateinit var eventId: String
    private var organizerMode: Boolean = false
    private lateinit var repository: EventCommentRepository
    private lateinit var adapter: EventCommentsAdapter
    private var commentsListener: ListenerRegistration? = null

    private lateinit var emptyText: TextView
    private lateinit var commentInput: EditText
    private lateinit var postButton: Button
    private lateinit var modeSubtitle: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = requireArguments().getString(ARG_EVENT_ID).orEmpty()
        organizerMode = requireArguments().getBoolean(ARG_ORGANIZER_MODE, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_event_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = EventCommentRepository(requireContext())

        val backButton = view.findViewById<ImageButton>(R.id.commentsBackButton)
        emptyText = view.findViewById(R.id.commentsEmptyText)
        commentInput = view.findViewById(R.id.commentInput)
        postButton = view.findViewById(R.id.postCommentButton)
        modeSubtitle = view.findViewById(R.id.commentsModeSubtitle)
        recyclerView = view.findViewById(R.id.commentsRecyclerView)

        adapter = EventCommentsAdapter { comment ->
            showDeleteDialog(comment.id)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (organizerMode) {
            modeSubtitle.text = "Organizer view"
            repository.canManageComments(
                eventId,
                object : EventCommentRepository.BooleanCallback {
                    override fun onResult(value: Boolean) {
                        adapter.setCanDelete(value)
                        if (!value) {
                            Toast.makeText(requireContext(), "You are not the organizer of this event", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                object : EventCommentRepository.ErrorCallback {
                    override fun onError(e: Exception) {
                        Toast.makeText(requireContext(), e.message ?: "Error", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } else {
            modeSubtitle.text = "Entrant view"
            adapter.setCanDelete(false)
        }

        postButton.setOnClickListener {
            val text = commentInput.text.toString()
            repository.addComment(
                eventId,
                text,
                object : EventCommentRepository.SuccessCallback {
                    override fun onSuccess() {
                        if (!isAdded) return
                        commentInput.setText("")
                    }
                },
                object : EventCommentRepository.ErrorCallback {
                    override fun onError(e: Exception) {
                        if (!isAdded) return
                        Toast.makeText(requireContext(), e.message ?: "Failed to post comment", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        commentsListener = repository.listenToComments(
            eventId,
            object : EventCommentRepository.CommentsCallback {
                override fun onUpdate(comments: List<com.example.spiral_event_lottery_app.model.EventComment>) {
                    if (!isAdded) return
                    adapter.submitList(comments)
                    emptyText.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.post {
                        if (comments.isNotEmpty()) {
                            recyclerView.scrollToPosition(comments.size - 1)
                        }
                    }
                }
            },
            object : EventCommentRepository.ErrorCallback {
                override fun onError(e: Exception) {
                    if (!isAdded) return
                    Toast.makeText(requireContext(), e.message ?: "Failed to load comments", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showDeleteDialog(commentId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Comment")
            .setMessage("Delete this comment?")
            .setPositiveButton("Delete") { _, _ ->
                repository.deleteComment(
                    eventId,
                    commentId,
                    object : EventCommentRepository.SuccessCallback {
                        override fun onSuccess() {
                            if (!isAdded) return
                            Toast.makeText(requireContext(), "Comment deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    object : EventCommentRepository.ErrorCallback {
                        override fun onError(e: Exception) {
                            if (!isAdded) return
                            Toast.makeText(requireContext(), e.message ?: "Failed to delete comment", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        commentsListener?.remove()
        commentsListener = null
        super.onDestroyView()
    }
}