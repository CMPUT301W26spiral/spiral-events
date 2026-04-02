package com.example.spiral_event_lottery_app.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.ui.details.EventDetailsFragment
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.model.User
import com.example.spiral_event_lottery_app.ui.oevent.EventDetailsOFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * HomeFragment displays a list of all open events.
 * It identifies if the current user is the organizer of an event to show different options.
 * Now supports smart sorting based on user interests.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventAdapter
    private lateinit var repository: EventRepository
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var searchEditText: EditText
    private lateinit var filterButton: ImageButton
    
    private lateinit var chipAll: TextView
    private lateinit var chipOpen: TextView
    private lateinit var chipFull: TextView

    private var startDate: Date? = null
    private var endDate: Date? = null
    
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventRepository(requireContext())
        recyclerView = view.findViewById(R.id.eventsRecyclerView)
        searchEditText = view.findViewById(R.id.searchEditText)
        filterButton = view.findViewById(R.id.filterButton)
        
        chipAll = view.findViewById(R.id.chipAll)
        chipOpen = view.findViewById(R.id.chipOpen)
        chipFull = view.findViewById(R.id.chipFull)
        
        val deviceId = DeviceIdProvider.getDeviceId(requireContext())

        adapter = EventAdapter(
            allEvents = emptyList(),
            deviceId = deviceId,
            onDetailsClicked = { event ->
                parentFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer,
                        EventDetailsOFragment.newInstance(event.id),
                        "details_screen")
                    .addToBackStack("details")
                    .commit()
            },
            onSignUpClicked = { event ->
                parentFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer,
                        EventDetailsFragment.newInstance(event.id),
                        "details_screen")
                    .addToBackStack("details")
                    .commit()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Set up search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        filterButton.setOnClickListener {
            showDateRangePickerDialog()
        }

        setupChips()

        val scanBtn = view.findViewById<android.widget.Button>(R.id.scanButton)
        scanBtn.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.example.spiral_event_lottery_app.QR_scanner::class.java)
            startActivity(intent)
        }
        
        loadCurrentUser()
    }
    
    /**
     * Loads the current user's profile to apply interest-based sorting.
     * Public so it can be triggered by MainActivity when interests change.
     */
    fun loadCurrentUser() {
        if (!isAdded) return
        val uid = DeviceIdProvider.getDeviceId(requireContext())
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists() && isAdded) {
                val user = doc.toObject(User::class.java)
                adapter.setCurrentUser(user)
            }
        }
    }

    private fun setupChips() {
        chipAll.setOnClickListener {
            updateChipSelection(EventAdapter.FilterStatus.ALL)
        }
        chipOpen.setOnClickListener {
            updateChipSelection(EventAdapter.FilterStatus.OPEN)
        }
        chipFull.setOnClickListener {
            updateChipSelection(EventAdapter.FilterStatus.FULL)
        }
    }

    private fun updateChipSelection(status: EventAdapter.FilterStatus) {
        adapter.setStatusFilter(status)
        
        chipAll.setBackgroundResource(if (status == EventAdapter.FilterStatus.ALL) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
        chipAll.setTextColor(if (status == EventAdapter.FilterStatus.ALL) 0xFFFFFFFF.toInt() else 0xFF1F1F1F.toInt())
        
        chipOpen.setBackgroundResource(if (status == EventAdapter.FilterStatus.OPEN) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
        chipOpen.setTextColor(if (status == EventAdapter.FilterStatus.OPEN) 0xFFFFFFFF.toInt() else 0xFF1F1F1F.toInt())
        
        chipFull.setBackgroundResource(if (status == EventAdapter.FilterStatus.FULL) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
        chipFull.setTextColor(if (status == EventAdapter.FilterStatus.FULL) 0xFFFFFFFF.toInt() else 0xFF1F1F1F.toInt())
    }

    private fun showDateRangePickerDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_date_range_picker, null)
        val btnStartDate = dialogView.findViewById<Button>(R.id.btnStartDate)
        val btnEndDate = dialogView.findViewById<Button>(R.id.btnEndDate)
        val btnClear = dialogView.findViewById<Button>(R.id.btnClear)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnApply = dialogView.findViewById<Button>(R.id.btnApply)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        var tempStart = startDate
        var tempEnd = endDate

        tempStart?.let { btnStartDate.text = dateFormat.format(it) }
        tempEnd?.let { btnEndDate.text = dateFormat.format(it) }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnStartDate.setOnClickListener {
            val cal = Calendar.getInstance()
            tempStart?.let { cal.time = it }
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val newDate = Calendar.getInstance()
                newDate.set(y, m, d, 0, 0, 0)
                newDate.set(Calendar.MILLISECOND, 0)
                tempStart = newDate.time
                btnStartDate.text = dateFormat.format(tempStart!!)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnEndDate.setOnClickListener {
            val cal = Calendar.getInstance()
            tempEnd?.let { cal.time = it }
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val newDate = Calendar.getInstance()
                newDate.set(y, m, d, 23, 59, 59)
                newDate.set(Calendar.MILLISECOND, 999)
                tempEnd = newDate.time
                btnEndDate.text = dateFormat.format(tempEnd!!)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnClear.setOnClickListener {
            tempStart = null
            tempEnd = null
            btnStartDate.text = "Select Start Date"
            btnEndDate.text = "Select End Date"
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnApply.setOnClickListener {
            startDate = tempStart
            endDate = tempEnd
            adapter.setDateRangeFilter(startDate, endDate)
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        listenerRegistration = repository.listenToOpenEvents(
            { events -> 
                adapter.submitList(events)
                val currentSearch = searchEditText.text.toString()
                if (currentSearch.isNotEmpty()) {
                    adapter.filter(currentSearch)
                }
            },
            { }
        )
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
