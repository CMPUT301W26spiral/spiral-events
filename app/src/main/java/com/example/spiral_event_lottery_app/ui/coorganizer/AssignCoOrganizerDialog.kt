package com.example.spiral_event_lottery_app.ui.coorganizer

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.example.spiral_event_lottery_app.data.CoOrganizerNotificationHelper
import com.example.spiral_event_lottery_app.data.CoOrganizerRepository
import com.example.spiral_event_lottery_app.model.User

class AssignCoOrganizerDialog(
    private val context: Context,
    private val eventId: String,
    private val eventName: String
) {

    private val repo = CoOrganizerRepository(context)

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