package com.example.spiral_event_lottery_app.ui.odetails

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fragment that displays the winners of a lottery draw for a specific event.
 *
 * It retrieves the list of users from the 'selected_list' subcollection in Firestore,
 * fetches their names from the 'users' collection, and presents them using a RecyclerView.
 */
class DrawResultsFragment : Fragment(R.layout.fragment_draw_results) {

    companion object {
        private const val ARG_EVENT_ID = "event_id"

        /**
         * Creates a new instance of DrawResultsFragment for the specified event.
         *
         * @param eventId The unique identifier of the event.
         * @return A new instance of DrawResultsFragment.
         */
        fun newInstance(eventId: String) = DrawResultsFragment().apply {
            arguments = Bundle().apply { putString(ARG_EVENT_ID, eventId) }
        }
    }

    private lateinit var eventId: String
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: SelectedUsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = requireArguments().getString(ARG_EVENT_ID)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.selectedUsersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = SelectedUsersAdapter(listOf())
        recyclerView.adapter = adapter

        val returnBtn = view.findViewById<Button>(R.id.selectedNotif)
        returnBtn.setOnClickListener {
            // Pop twice: back from Results to DoDraw, then back from DoDraw to EventDetailsO
            parentFragmentManager.popBackStack()
            parentFragmentManager.popBackStack()
        }

        // 1. Fetch selected users from the event's 'selected_list' subcollection
        db.collection("events")
            .document(eventId)
            .collection("selected_list")
            .get()
            .addOnSuccessListener { snapshot ->
                val selectedDocs = snapshot.documents
                if (selectedDocs.isEmpty()) return@addOnSuccessListener

                val selectedUsersList = mutableListOf<SelectedUser>()
                var processedCount = 0

                for (doc in selectedDocs) {
                    val userId = doc.id
                    // Get 'selectedAt' stored in the selected_list subcollection
                    val selectedAt = doc.getLong("selectedAt")

                    // 2. Fetch full user details (name/photo) from the top-level 'users' collection
                    db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
                        val name = userDoc.getString("name") ?: "Unknown User"
                        val photo = userDoc.getString("photoUrl")


                        // Populate the SelectedUser object with data from both queries
                        selectedUsersList.add(SelectedUser(
                            userId = userId,
                            name = name,
                            profileUrl = photo,
                            selectedAt = selectedAt
                        ))

                        processedCount++
                        // Once all lookups for all selected users are done, update the UI
                        if (processedCount == selectedDocs.size) {
                            // Optionally sort by selection time
                            selectedUsersList.sortBy { it.selectedAt }
                            adapter.submitList(selectedUsersList)
                        }
                    }
                }
            }
    }
}