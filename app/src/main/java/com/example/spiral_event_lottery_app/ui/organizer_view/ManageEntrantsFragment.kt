package com.example.spiral_event_lottery_app.ui.organizer_view

import android.os.Bundle
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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.data.NotificationManager
import com.example.spiral_event_lottery_app.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.ContextCompat

/**
 * ManageEntrantsFragment displays the list of entrants for a specific event.
 * Handles custom mass notifications and private invitations.
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

    private var currentTab = "waiting"

    companion object {
        fun newInstance(eventId: String): ManageEntrantsFragment {
            return ManageEntrantsFragment().apply {
                arguments = Bundle().apply { putString("event_id", eventId) }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        eventId = arguments?.getString("event_id") ?: ""

        // Bind buttons
        btnInvitePrivate = view.findViewById(R.id.btnInvitePrivate)
        btnNotifyAll = view.findViewById(R.id.btnNotifyAll)
        
        btnInvited = view.findViewById(R.id.btnInvited)
        btnWaiting = view.findViewById(R.id.btnWaiting)
        btnCancelled = view.findViewById(R.id.btnCancelled)
        invitedRecycler = view.findViewById(R.id.invitedRecycler)
        waitingRecycler = view.findViewById(R.id.waitingRecycler)
        cancelledRecycler = view.findViewById(R.id.cancelledRecycler)
        waitingCountText = view.findViewById(R.id.waitingCountText)
        btnExportCsv = view.findViewById(R.id.btnExportCsv)

        invitedRecycler.layoutManager = LinearLayoutManager(requireContext())
        waitingRecycler.layoutManager = LinearLayoutManager(requireContext())
        cancelledRecycler.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<View>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnInvited.setOnClickListener { showTab("invited") }
        btnWaiting.setOnClickListener { showTab("waiting") }
        btnCancelled.setOnClickListener { showTab("cancelled") }

        // Logic for custom mass notification
        // US 02.07.01, 02.07.02, 02.07.03 logic
        btnNotifyAll.setOnClickListener {
            showCustomNotificationDialog()
        }

        // US 01.05.06 logic
        btnInvitePrivate.setOnClickListener {
            showInviteDialog()
        }

        btnExportCsv.setOnClickListener { }

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

    /**
     * Shows a dialog allowing the organizer to compose a custom message for entrants.
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
     * Sends the custom message to all entrants in the currently visible list.
     */
    private fun notifyAllWithCustomMessage(message: String) {
        val collectionPath = when (currentTab) {
            "invited" -> "selected_list"
            "waiting" -> "waitlist"
            "cancelled" -> "canceled_list"
            else -> "waitlist"
        }

        db.collection("events").document(eventId).get().addOnSuccessListener { eventDoc ->
            val eventName = eventDoc.getString("name") ?: "Event"
            
            db.collection("events").document(eventId).collection(collectionPath).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        Toast.makeText(requireContext(), "List is empty.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    snapshot.documents.forEach { doc ->
                        NotificationManager.sendNotification(
                            doc.id,
                            "Organizer Update",
                            message,
                            "ORGANIZER",
                            eventName,
                            eventId
                        )
                    }
                    Toast.makeText(requireContext(), "Message sent successfully!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Opens a dialog to search for and invite a specific entrant to a private event.
     * Implements US 01.05.06.
     */
    private fun showInviteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_invite_user, null)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.inviteCategorySpinner)
        val searchInput = dialogView.findViewById<EditText>(R.id.inviteSearchInput)
        val searchResultRecycler = dialogView.findViewById<RecyclerView>(R.id.searchResultRecycler)

        // Setup Spinner
        val categories = listOf("Name", "Email", "Phone")
        categorySpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)

        // Setup Search Results
        val searchAdapter = UserSearchAdapter { user ->
            confirmInviteUser(user)
        }
        searchResultRecycler.layoutManager = LinearLayoutManager(requireContext())
        searchResultRecycler.adapter = searchAdapter

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Private Event Invitation")
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .show()

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length >= 1) {
                    performUserSearch(query, categorySpinner.selectedItem.toString().lowercase(), searchAdapter)
                } else {
                    searchAdapter.submitList(emptyList())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun performUserSearch(query: String, category: String, adapter: UserSearchAdapter) {
        val field = when(category) {
            "name" -> "name"
            "email" -> "email"
            "phone" -> "phoneNumber"
            else -> "name"
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

    private fun inviteUserToWaitlist(user: User) {
        val waitlistRef = db.collection("events").document(eventId).collection("waitlist").document(user.deviceId)

        db.runTransaction { transaction ->
            if (transaction.get(waitlistRef).exists()) throw Exception("ALREADY_IN_WAITLIST")

            val eventRef = db.collection("events").document(eventId)
            val eventDoc = transaction.get(eventRef)
            val currentCount = eventDoc.getLong("waiting_count") ?: 0L

            transaction.set(waitlistRef, mapOf("device_id" to user.deviceId, "joined_at" to Timestamp.now()))
            transaction.update(eventRef, "waiting_count", currentCount + 1)
            eventDoc.getString("name") ?: "Private Event"
        }.addOnSuccessListener { eventName ->
            NotificationManager.sendNotification(user.deviceId, "Private Invitation", "You have been invited to join the waiting list for $eventName!", "ORGANIZER", eventName, eventId)
            Toast.makeText(requireContext(), "Invitation sent successfully!", Toast.LENGTH_SHORT).show()
            loadWaitingEntrants() // Refresh UI
        }.addOnFailureListener { e ->
            val msg = if (e.message == "ALREADY_IN_WAITLIST") "User is already on the waitlist." else "Failed to invite user: ${e.message}"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private inner class UserSearchAdapter(private val onItemClick: (User) -> Unit) :
        RecyclerView.Adapter<UserSearchAdapter.VH>() {
        private var users = listOf<User>()

        fun submitList(newList: List<User>) {
            users = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user_search_result, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val user = users[position]
            holder.userName.text = user.name
            holder.userDetail.text = user.email ?: user.phoneNumber
            holder.itemView.setOnClickListener { onItemClick(user) }
        }

        override fun getItemCount() = users.size

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userDetail: TextView = itemView.findViewById(R.id.userDetail)
        }
    }

    private fun showTab(tab: String) {
        currentTab = tab
        invitedRecycler.visibility = if (tab == "invited") View.VISIBLE else View.GONE
        waitingRecycler.visibility = if (tab == "waiting") View.VISIBLE else View.GONE
        cancelledRecycler.visibility = if (tab == "cancelled") View.VISIBLE else View.GONE

        btnNotifyAll.text = "Notify All ${tab.replaceFirstChar { it.uppercase() }} Entrants"

        val activeColor = ContextCompat.getColor(requireContext(), R.color.primary_green)
        val inactiveColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val activeText = ContextCompat.getColor(requireContext(), android.R.color.white)
        val inactiveText = ContextCompat.getColor(requireContext(), R.color.primary_green)

        listOf(btnInvited, btnWaiting, btnCancelled).forEach { btn ->
            btn.setBackgroundColor(inactiveColor)
            btn.setTextColor(inactiveText)
        }

        val activeBtn = when (tab) {
            "invited" -> btnInvited
            "waiting" -> btnWaiting
            else -> btnCancelled
        }
        activeBtn.setBackgroundColor(activeColor)
        activeBtn.setTextColor(activeText)
    }

    private fun loadWaitingEntrants() {
        db.collection("events").document(eventId).collection("waitlist").get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.id }
                waitingCountText.text = "${deviceIds.size} People on Waiting List"
                resolveNames(deviceIds) { names -> waitingRecycler.adapter = EntrantAdapter(names) }
            }
    }

    private fun loadInvitedEntrants() {
        db.collection("events").document(eventId).collection("selected_list").get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.id }
                resolveNames(deviceIds) { names -> invitedRecycler.adapter = EntrantAdapter(names) }
            }
    }

    private fun loadCancelledEntrants() {
        // Changed "cancelled_list" to "canceled_list" to match EventRepository
        db.collection("events").document(eventId).collection("canceled_list").get()
            .addOnSuccessListener { snapshot ->
                val deviceIds = snapshot.documents.map { it.id }
                resolveNames(deviceIds) { names ->
                    cancelledRecycler.adapter = EntrantAdapter(names)
                }
            }
    }

    private fun resolveNames(deviceIds: List<String>, onComplete: (List<String>) -> Unit) {
        if (deviceIds.isEmpty()) {
            onComplete(emptyList())
            return
        }
        val names = MutableList(deviceIds.size) { deviceIds[it] }
        var resolved = 0
        for ((index, deviceId) in deviceIds.withIndex()) {
            db.collection("users").document(deviceId).get()
                .addOnSuccessListener { userDoc ->
                    if (userDoc.exists()) names[index] = userDoc.getString("name") ?: deviceId
                    resolved++
                    if (resolved == deviceIds.size) onComplete(names)
                }
                .addOnFailureListener {
                    resolved++
                    if (resolved == deviceIds.size) onComplete(names)
                }
        }
    }
}
