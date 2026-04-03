package com.example.spiral_event_lottery_app.ui.organizer_view

import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.example.spiral_event_lottery_app.model.User
import com.example.spiral_event_lottery_app.ui.coorganizer.AssignCoOrganizerDialog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * ManageEntrantsFragment displays and manages entrants for a specific event.
 *
 * Shows three tabs:
 * - Waiting: entrants on the waitlist, not yet drawn
 * - Invited: entrants drawn from the lottery, with pending or accepted status
 * - Cancelled: entrants who declined or were cancelled by the organizer
 *
 * User Stories covered:
 * - US 02.02.01: View waiting list
 * - US 02.06.01: View invited/chosen entrants
 * - US 02.06.02: View cancelled entrants
 * - US 02.06.03: View final enrolled entrants (accepted)
 * - US 02.06.04: Cancel entrants who did not sign up
 * - US 02.06.05: Export final entrant list as CSV
 * - US 02.07.01/02/03: Send notifications to entrants by tab
 * - US 01.05.06: Invite specific entrant to private event
 * - US 02.09.01: Assign entrant as co-organizer
 */
class ManageEntrantsFragment : Fragment(R.layout.fragment_manage_entrants) {

    private lateinit var eventId: String
    private val db = FirebaseFirestore.getInstance()

    private lateinit var invitedRecycler: RecyclerView
    private lateinit var waitingRecycler: RecyclerView
    private lateinit var cancelledRecycler: RecyclerView
    private lateinit var btnInvited: Button
    private lateinit var btnWaiting: Button
    private lateinit var btnCancelled: Button
    private lateinit var waitingCountText: TextView
    private lateinit var btnNotifyAll: Button
    private lateinit var btnInvitePrivate: Button
    private lateinit var btnExportCsv: Button

    /** Tracks which tab is currently visible */
    private var currentTab = "waiting"

    /** Cached event name used for notifications */
    private var eventName = "Event"

    companion object {
        /**
         * Creates a new instance of ManageEntrantsFragment with the given event ID.
         *
         * @param eventId The Firestore document ID of the event to manage
         * @return A configured ManageEntrantsFragment instance
         */
        fun newInstance(eventId: String): ManageEntrantsFragment {
            return ManageEntrantsFragment().apply {
                arguments = Bundle().apply { putString("event_id", eventId) }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        eventId = arguments?.getString("event_id") ?: ""

        btnInvitePrivate = view.findViewById(R.id.btnInvitePrivate)
        btnNotifyAll     = view.findViewById(R.id.btnNotifyAll)
        btnInvited       = view.findViewById(R.id.btnInvited)
        btnWaiting       = view.findViewById(R.id.btnWaiting)
        btnCancelled     = view.findViewById(R.id.btnCancelled)
        invitedRecycler  = view.findViewById(R.id.invitedRecycler)
        waitingRecycler  = view.findViewById(R.id.waitingRecycler)
        cancelledRecycler = view.findViewById(R.id.cancelledRecycler)
        waitingCountText = view.findViewById(R.id.waitingCountText)
        btnExportCsv     = view.findViewById(R.id.btnExportCsv)

        invitedRecycler.layoutManager   = LinearLayoutManager(requireContext())
        waitingRecycler.layoutManager   = LinearLayoutManager(requireContext())
        cancelledRecycler.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<View>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnInvited.setOnClickListener   { showTab("invited") }
        btnWaiting.setOnClickListener   { showTab("waiting") }
        btnCancelled.setOnClickListener { showTab("cancelled") }

        // US 02.07.01, 02.07.02, 02.07.03
        btnNotifyAll.setOnClickListener { showCustomNotificationDialog() }

        // US 01.05.06
        btnInvitePrivate.setOnClickListener { showInviteDialog() }

        // US 02.06.05
        btnExportCsv.setOnClickListener { exportEnrolledAsCsv() }

        loadEventName()

        // Hide invite button if event is public
        db.collection("events").document(eventId).get().addOnSuccessListener { doc ->
            if (isAdded) {
                val isPublic = doc.getBoolean("isPublic") ?: true
                btnInvitePrivate.visibility = if (isPublic) View.GONE else View.VISIBLE
            }
        }

        showTab("waiting")
        loadWaitingEntrants()
        loadInvitedEntrants()
        loadCancelledEntrants()
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    /**
     * Fetches the event name from Firestore and caches it for use in notifications.
     */
    private fun loadEventName() {
        db.collection("events").document(eventId).get()
            .addOnSuccessListener { doc ->
                eventName = doc.getString("name") ?: "Event"
            }
    }

    /**
     * Loads entrants from the waitlist subcollection and displays them.
     * Implements US 02.02.01.
     */
    private fun loadWaitingEntrants() {
        db.collection("events").document(eventId).collection("waitlist").get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.id }
                val statusMap = snapshot.documents.associate { doc ->
                    doc.id to (doc.getString("status") ?: "joined")
                }
                waitingCountText.text = "${deviceIds.size} People on Waiting List"
                resolveUsers(deviceIds) { users ->
                    waitingRecycler.adapter = EntrantAdapter(
                        users,
                        statusMap = statusMap,
                        onRemove = null,
                        onAssignCoOrganizer = { user ->
                            AssignCoOrganizerDialog(requireContext(), eventId, eventName).show(user)
                        }
                    )
                }
            }
    }

    /**
     * Loads entrants from selected_list with status "pending" or "accepted".
     * The remove button allows the organizer to cancel a pending entrant (US 02.06.04).
     * Implements US 02.06.01 and US 02.06.03.
     */
    private fun loadInvitedEntrants() {
        db.collection("events").document(eventId).collection("selected_list")
            .whereIn("status", listOf("pending", "accepted", "invited"))
            .get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.id }
                val statusMap = snapshot.documents.associate { doc ->
                    doc.id to (doc.getString("status") ?: "pending")
                }
                resolveUsers(deviceIds) { users ->
                    invitedRecycler.adapter = EntrantAdapter(
                        users,
                        statusMap = statusMap,
                        onRemove = { user -> cancelEntrant(user.deviceId) },
                        onAssignCoOrganizer = { user ->
                            AssignCoOrganizerDialog(requireContext(), eventId, eventName).show(user)
                        }
                    )
                }
            }
    }

    /**
     * Loads entrants from selected_list with status "declined" or "cancelled".
     * Implements US 02.06.02.
     */
    private fun loadCancelledEntrants() {
        db.collection("events").document(eventId).collection("canceled_list")
            .whereIn("status", listOf("declined", "cancelled"))
            .get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.id }
                val statusMap = snapshot.documents.associate { doc ->
                    doc.id to (doc.getString("status") ?: "cancelled")
                }
                resolveUsers(deviceIds) { users ->
                    cancelledRecycler.adapter = EntrantAdapter(
                        users,
                        statusMap = statusMap
                    )
                }
            }
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     * Cancels a specific entrant by moving them from selected_list to canceled_list.
     * Shows a confirmation dialog before cancelling.
     * Implements US 02.06.04.
     *
     * @param deviceId The device ID of the entrant to cancel
     */
    private fun cancelEntrant(deviceId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Entrant")
            .setMessage("Are you sure you want to cancel this entrant?")
            .setPositiveButton("Yes") { _, _ ->
                val eventRef = db.collection("events").document(eventId)
                val selectedRef = eventRef.collection("selected_list").document(deviceId)
                val canceledRef = eventRef.collection("canceled_list").document(deviceId)

                db.runTransaction { transaction ->
                    val snapshot = transaction.get(selectedRef)
                    if (snapshot.exists()) {
                        val data = snapshot.data?.toMutableMap() ?: mutableMapOf()
                        data["status"] = "cancelled"
                        data["cancelledAt"] = Timestamp.now()

                        transaction.set(canceledRef, data)
                        transaction.delete(selectedRef)
                    }
                    null
                }.addOnSuccessListener {
                    Toast.makeText(requireContext(), "Entrant cancelled.", Toast.LENGTH_SHORT).show()
                    loadInvitedEntrants()
                    loadCancelledEntrants()
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to cancel: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    /**
     * Exports the final list of accepted entrants to a CSV file in the Downloads folder.
     * Only entrants with status "accepted" are included.
     * Implements US 02.06.05.
     */
    private fun exportEnrolledAsCsv() {
        db.collection("events").document(eventId).collection("selected_list")
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(requireContext(), "No enrolled entrants to export.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val deviceIds = snapshot.documents.map { it.id }
                resolveUsers(deviceIds) { users ->
                    val csvBuilder = StringBuilder()
                    csvBuilder.appendLine("Name,Device ID,Status")
                    users.forEach { user ->
                        csvBuilder.appendLine("${user.name},${user.deviceId},accepted")
                    }
                    try {
                        val fileName = "enrolled_entrants_$eventId.csv"
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }
                        val resolver = requireContext().contentResolver
                        val uri = resolver.insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            contentValues
                        )
                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use {
                                it.write(csvBuilder.toString().toByteArray())
                            }
                            contentValues.clear()
                            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)
                            Toast.makeText(
                                requireContext(),
                                "CSV saved to Downloads: $fileName",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(requireContext(), "Failed to create CSV file.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    /**
     * Shows a dialog allowing the organizer to compose a custom notification message
     * for all entrants in the currently visible tab.
     * Implements US 02.07.01, 02.07.02, 02.07.03.
     */
    private fun showCustomNotificationDialog() {
        val input = EditText(requireContext())
        input.hint = "Enter update message..."
        input.setPadding(48, 32, 48, 32)

        AlertDialog.Builder(requireContext())
            .setTitle("Send Notification to $currentTab list")
            .setMessage("Type the message you want to send to everyone in the current list.")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val customMessage = input.text.toString().trim()
                if (customMessage.isNotEmpty()) {
                    notifyAllWithCustomMessage(customMessage)
                } else {
                    Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Sends a custom notification message to all entrants in the currently visible tab.
     * Routes to the correct Firestore collection based on the active tab.
     *
     * @param message The custom message to send to all entrants in the current tab
     */
    private fun notifyAllWithCustomMessage(message: String) {
        db.collection("events").document(eventId).get().addOnSuccessListener { eventDoc ->
            val currentEventName = eventDoc.getString("name") ?: "Event"

            if (currentTab == "waiting") {
                db.collection("events").document(eventId).collection("waitlist").get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            Toast.makeText(requireContext(), "List is empty.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        snapshot.documents.forEach { doc ->
                            NotificationManager.sendNotification(
                                doc.id, "Organizer Update", message,
                                "ORGANIZER", currentEventName, eventId
                            )
                        }
                        Toast.makeText(requireContext(), "Message sent successfully!", Toast.LENGTH_SHORT).show()
                    }
            } else {
                val collectionName = if (currentTab == "cancelled") "canceled_list" else "selected_list"
                val statusFilters = when (currentTab) {
                    "invited"   -> listOf("pending", "accepted", "invited")
                    "cancelled" -> listOf("declined", "cancelled")
                    else        -> listOf("pending", "accepted")
                }
                db.collection("events").document(eventId).collection(collectionName)
                    .whereIn("status", statusFilters).get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            Toast.makeText(requireContext(), "List is empty.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        snapshot.documents.forEach { doc ->
                            NotificationManager.sendNotification(
                                doc.id, "Organizer Update", message,
                                "ORGANIZER", currentEventName, eventId
                            )
                        }
                        Toast.makeText(requireContext(), "Message sent successfully!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    /**
     * Opens a dialog to search for and invite a specific entrant to a private event.
     * Supports searching by name, email, or phone number.
     * Implements US 01.05.06.
     */
    private fun showInviteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_invite_user, null)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.inviteCategorySpinner)
        val searchInput = dialogView.findViewById<EditText>(R.id.inviteSearchInput)
        val searchResultRecycler = dialogView.findViewById<RecyclerView>(R.id.searchResultRecycler)

        val categories = listOf("Name", "Email", "Phone")
        categorySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )

        val searchAdapter = UserSearchAdapter { user -> confirmInviteUser(user) }
        searchResultRecycler.layoutManager = LinearLayoutManager(requireContext())
        searchResultRecycler.adapter = searchAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("Private Event Invitation")
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .show()

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    performUserSearch(
                        query,
                        categorySpinner.selectedItem.toString().lowercase(),
                        searchAdapter
                    )
                } else {
                    searchAdapter.submitList(emptyList())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Searches Firestore users collection by the given field and query string.
     *
     * @param query    The search string entered by the organizer
     * @param category The field to search by: "name", "email", or "phone"
     * @param adapter  The adapter to update with search results
     */
    private fun performUserSearch(query: String, category: String, adapter: UserSearchAdapter) {
        val field = when (category) {
            "name"  -> "name"
            "email" -> "email"
            "phone" -> "phoneNumber"
            else    -> "name"
        }
        val currentDeviceId = DeviceIdProvider.getDeviceId(requireContext())
        db.collection("users")
            .whereGreaterThanOrEqualTo(field, query)
            .whereLessThanOrEqualTo(field, query + "\uf8ff")
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                    .filter { it.deviceId != currentDeviceId }
                adapter.submitList(users)
            }
    }

    /**
     * Shows a confirmation dialog before inviting a user to the private event waitlist.
     *
     * @param user The user to potentially invite
     */
    private fun confirmInviteUser(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Invitation")
            .setMessage("Add ${user.name} to the waiting list for this event?")
            .setPositiveButton("Invite") { _, _ ->
                inviteUserToWaitlist(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Adds the given user to the event waitlist in Firestore and sends them a notification.
     * Uses a transaction to safely increment the waiting_count.
     * Implements US 01.05.06.
     *
     * @param user The user to invite to the waitlist
     */
    private fun inviteUserToWaitlist(user: User) {
        val waitlistRef = db.collection("events").document(eventId)
            .collection("waitlist").document(user.deviceId)

        db.runTransaction { transaction ->
            if (transaction.get(waitlistRef).exists()) throw Exception("ALREADY_IN_WAITLIST")
            val eventRef = db.collection("events").document(eventId)
            val eventDoc = transaction.get(eventRef)
            val currentCount = eventDoc.getLong("waiting_count") ?: 0L
            transaction.set(
                waitlistRef,
                mapOf(
                    "device_id" to user.deviceId,
                    "joined_at" to Timestamp.now(),
                    "status" to "pending" // Private invitation starts as pending
                )
            )
            transaction.update(eventRef, "waiting_count", currentCount + 1)
            eventDoc.getString("name") ?: "Private Event"
        }.addOnSuccessListener { name ->
            NotificationManager.sendNotification(
                user.deviceId, "Private Invitation",
                "You have been invited to join the waiting list for $name! Please accept or decline the invitation.",
                "ORGANIZER", name, eventId
            )
            Toast.makeText(requireContext(), "Invitation sent successfully!", Toast.LENGTH_SHORT).show()
            loadWaitingEntrants()
        }.addOnFailureListener { e ->
            val msg = if (e.message == "ALREADY_IN_WAITLIST")
                "User is already on the waitlist."
            else
                "Failed to invite user: ${e.message}"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    // -------------------------------------------------------------------------
    // Tab switching
    // -------------------------------------------------------------------------

    /**
     * Switches the visible RecyclerView to the selected tab and updates button styling.
     *
     * @param tab One of "invited", "waiting", or "cancelled"
     */
    private fun showTab(tab: String) {
        currentTab = tab
        invitedRecycler.visibility   = if (tab == "invited")   View.VISIBLE else View.GONE
        waitingRecycler.visibility   = if (tab == "waiting")   View.VISIBLE else View.GONE
        cancelledRecycler.visibility = if (tab == "cancelled") View.VISIBLE else View.GONE

        btnNotifyAll.text = "Notify All ${tab.replaceFirstChar { it.uppercase() }} Entrants"

        val activeColor   = ContextCompat.getColor(requireContext(), R.color.primary_green)
        val inactiveColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val activeText    = ContextCompat.getColor(requireContext(), android.R.color.white)
        val inactiveText  = ContextCompat.getColor(requireContext(), R.color.primary_green)

        listOf(btnInvited, btnWaiting, btnCancelled).forEach { btn ->
            btn.setBackgroundColor(inactiveColor)
            btn.setTextColor(inactiveText)
        }
        val activeBtn = when (tab) {
            "invited"   -> btnInvited
            "waiting"   -> btnWaiting
            else        -> btnCancelled
        }
        activeBtn.setBackgroundColor(activeColor)
        activeBtn.setTextColor(activeText)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves a list of device IDs to User objects by fetching each user document from Firestore.
     * Falls back to a User with just the device ID if the document doesn't exist.
     *
     * @param deviceIds List of device IDs to resolve
     * @param onComplete Callback returning the resolved list of User objects in the same order
     */
    private fun resolveUsers(deviceIds: List<String>, onComplete: (List<User>) -> Unit) {
        if (deviceIds.isEmpty()) {
            onComplete(emptyList())
            return
        }
        val users = MutableList(deviceIds.size) { index ->
            User(deviceId = deviceIds[index], name = deviceIds[index])
        }
        var resolved = 0
        for ((index, deviceId) in deviceIds.withIndex()) {
            db.collection("users").document(deviceId).get()
                .addOnSuccessListener { userDoc ->
                    if (userDoc.exists()) {
                        users[index] = User(
                            deviceId    = deviceId,
                            name        = userDoc.getString("name") ?: deviceId,
                            email       = userDoc.getString("email") ?: "",
                            phoneNumber = userDoc.getString("phoneNumber") ?: "",
                            photoUrl    = userDoc.getString("photoUrl"),
                            isAdmin     = userDoc.getBoolean("isAdmin") ?: false,
                            eventList   = mutableListOf()
                        )
                    }
                    resolved++
                    if (resolved == deviceIds.size) onComplete(users)
                }
                .addOnFailureListener {
                    resolved++
                    if (resolved == deviceIds.size) onComplete(users)
                }
        }
    }

    // -------------------------------------------------------------------------
    // Inner adapter for user search results
    // -------------------------------------------------------------------------

    /**
     * Inner RecyclerView adapter for displaying user search results in the invite dialog.
     *
     * @param onItemClick Callback invoked when a user result is tapped
     */
    private inner class UserSearchAdapter(private val onItemClick: (User) -> Unit) :
        RecyclerView.Adapter<UserSearchAdapter.VH>() {

        private var users = listOf<User>()

        /**
         * Updates the list of search results and refreshes the RecyclerView.
         *
         * @param newList The new list of users to display
         */
        fun submitList(newList: List<User>) {
            users = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_search_result, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val user = users[position]
            holder.userName.text   = user.name
            holder.userDetail.text = user.email ?: user.phoneNumber
            holder.itemView.setOnClickListener { onItemClick(user) }
        }

        override fun getItemCount() = users.size

        /**
         * ViewHolder for a single user search result row.
         */
        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView   = itemView.findViewById(R.id.userName)
            val userDetail: TextView = itemView.findViewById(R.id.userDetail)
        }
    }
}
