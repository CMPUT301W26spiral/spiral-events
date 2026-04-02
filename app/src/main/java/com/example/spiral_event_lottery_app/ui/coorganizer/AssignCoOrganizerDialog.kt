package com.example.spiral_event_lottery_app.ui.coorganizer

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.example.spiral_event_lottery_app.data.CoOrganizerNotificationHelper
import com.example.spiral_event_lottery_app.data.CoOrganizerRepository
import com.example.spiral_event_lottery_app.model.User


/**
 * AssignCoOrganizerDialog handles the UI flow for assigning an entrant
 * as a co-organizer for a specific event.
 *
 * This dialog prompts the organizer for confirmation before promoting a user.
 * Upon confirmation:
 * - The selected user is assigned as a co-organizer via CoOrganizerRepository
 * - The user is removed from the waitlist if applicable
 * - A notification is sent to the user informing them of their new role
 *
 * Used by: ManageEntrantsFragment
 *
 * User Stories:
 * - US 02.09.01: As an organizer, I want to assign an entrant as a co-organizer for my event.
 * - US 01.09.01: As an entrant, I want to receive a notification if I have been invited to be a co-organizer.
 */

class AssignCoOrganizerDialog(
    private val context: Context,
    private val eventId: String,
    private val eventName: String
) {

    private val repo = CoOrganizerRepository(context)
    /**
     * Displays the confirmation dialog for assigning a co-organizer.
     *
     * @param user The User object representing the entrant to be promoted
     */
    fun show(user: User) {
        AlertDialog.Builder(context)
            .setTitle("Assign Co-Organizer")
            .setMessage("Make ${user.name} a co-organizer?")
            .setPositiveButton("Assign") { _, _ ->
                repo.assignCoOrganizer(
                    eventId,
                    user.deviceId,
                    object : CoOrganizerRepository.SuccessCallback {
                        override fun onSuccess() {
                            CoOrganizerNotificationHelper.sendInvite(
                                user.deviceId,
                                eventId,
                                eventName
                            )
                            Toast.makeText(context, "Assigned successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    object : CoOrganizerRepository.ErrorCallback {
                        override fun onError(e: Exception) {
                            Toast.makeText(context, e.message ?: "Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}