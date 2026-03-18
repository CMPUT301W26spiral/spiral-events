package com.example.spiral_event_lottery_app.ui.odetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.spiral_event_lottery_app.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fragment responsible for initiating and performing the lottery draw for a specific event.
 *
 * This fragment allows an organizer to specify a limit for the number of entrants to be selected
 * from the waitlist. If no limit is provided, all entrants on the waitlist are selected.
 * It interacts with Firebase Firestore to read the waitlist and update the selected list.
 */
class DoDrawFragment : Fragment() {
    companion object {
        private const val ARG_EVENT_ID = "event_id"

        /**
         * Creates a new instance of DoDrawFragment with the provided event ID.
         *
         * @param eventId The unique identifier of the event.
         * @return A new instance of DoDrawFragment.
         */
        fun newInstance(eventId: String): DoDrawFragment {
            return DoDrawFragment().apply {
                arguments = Bundle().apply { putString(ARG_EVENT_ID, eventId) }
            }
        }
    }

    private lateinit var eventId: String
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = requireArguments().getString(ARG_EVENT_ID)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_do_draw, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find and set up the back button
        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        backButton?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Find the input field
        val entrantLimitEditText = view.findViewById<TextInputEditText>(
            R.id.entrantLimitInput
        )
        // Find the draw button
        val drawButton = view.findViewById<Button>(R.id.doDrawButton)
        val eventRef = db.collection("events").document(eventId)

        drawButton.setOnClickListener {
            eventRef.get().addOnSuccessListener { doc ->
                val alreadyDrawn = doc.getBoolean("lottery_done") ?: false
                if (alreadyDrawn) {
                    // Stop here if lottery was already run
                    Toast.makeText(
                        requireContext(),
                        "Lottery already completed!",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                // Get the user input as a string
                val inputText = entrantLimitEditText.text.toString()
                // Convert to Int? safely (null if blank or invalid)
                val entrantLimit = if (inputText.isNotBlank()) inputText.toIntOrNull() else null

                performLottery(eventId, entrantLimit) { success, message ->
                    if (success) {
                        eventRef.update("lottery_done", true)
                        Toast.makeText(requireContext(), "Lottery completed!", Toast.LENGTH_SHORT)
                            .show()
                        parentFragmentManager.beginTransaction()
                            .replace(
                                R.id.fragmentContainer,
                                DrawResultsFragment.newInstance(eventId)
                            )
                            .addToBackStack(null)
                            .commit()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Lottery failed: $message",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    /**
     * Executes the lottery logic by fetching the waitlist, selecting winners, and updating Firestore.
     *
     * @param eventId The ID of the event for which the draw is performed.
     * @param entrantLimit The maximum number of winners to select. If null, everyone on the waitlist is selected.
     * @param onComplete Callback invoked when the process finishes, returning success status and an optional error message.
     */
    private fun performLottery(
        eventId: String,
        entrantLimit: Int?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val eventRef = db.collection("events").document(eventId)
        val waitlistRef = eventRef.collection("waitlist")
        val selectedRef = eventRef.collection("selected_list")

        // Get all users on the waitlist
        waitlistRef.get().addOnSuccessListener { snapshot ->

            val waitlistUsers = snapshot.documents.map { it.id }

            if (waitlistUsers.isEmpty()) {
                onComplete(false, "No users in the waitlist")
                return@addOnSuccessListener
            }

            // Determine which users to select
            val selectedUsers = if (entrantLimit == null || entrantLimit >= waitlistUsers.size) {
                waitlistUsers // select all if no limit
            } else {
                waitlistUsers.shuffled().take(entrantLimit) // randomly select
            }

            // Move selected users to selected_list using a batch write for atomicity
            val batch = db.batch()
            selectedUsers.forEach { userId ->
                val docRef = selectedRef.document(userId)
                val waitlistDocRef = waitlistRef.document(userId)

                // stores the time in milliseconds when the "Go!" button was pressed
                batch.set(docRef, mapOf("selectedAt" to System.currentTimeMillis()))
                batch.delete(waitlistDocRef) // Remove from waitlist when moved to selected_list
            }
            
            // Decrement the waiting_count field on the main event document
            val remainingCount = waitlistUsers.size - selectedUsers.size
            batch.update(eventRef, "waiting_count", remainingCount.toLong())

            batch.commit()
                .addOnSuccessListener { onComplete(true, null) }
                .addOnFailureListener { e -> onComplete(false, e.message) }

        }.addOnFailureListener { e ->
            onComplete(false, e.message)
        }
    }
}
