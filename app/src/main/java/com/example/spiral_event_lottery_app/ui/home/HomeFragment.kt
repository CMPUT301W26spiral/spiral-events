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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.ui.details.EventDetailsFragment
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.ui.oevent.EventDetailsOFragment
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * HomeFragment displays a list of all open events.
 * It identifies if the current user is the organizer of an event to show different options.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventAdapter
    private lateinit var repository: EventRepository
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var searchEditText: EditText
    private lateinit var filterButton: ImageButton

    private var startDate: Date? = null
    private var endDate: Date? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventRepository(requireContext())
        recyclerView = view.findViewById(R.id.eventsRecyclerView)
        searchEditText = view.findViewById(R.id.searchEditText)
        filterButton = view.findViewById(R.id.filterButton)
        
        // Retrieve the current device ID to check against organizerId
        val deviceId = DeviceIdProvider.getDeviceId(requireContext())

        adapter = EventAdapter(
            allEvents = emptyList(),
            deviceId = deviceId,
            onDetailsClicked = { event ->
                // Navigate to Organizer Details if the user owns the event
                parentFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer,
                        EventDetailsOFragment.newInstance(event.id),
                        "details_screen")
                    .addToBackStack("details")
                    .commit()
            },
            onSignUpClicked = { event ->
                // Navigate to Entrant Details for signing up
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

        // 1. Find the Scan Button using the ID from your XML
        val scanBtn = view.findViewById<android.widget.Button>(R.id.scanButton)
        // 2. Set the click listener to open your camera
        scanBtn.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.example.spiral_event_lottery_app.QR_scanner::class.java)
            startActivity(intent)
        }
    }

    private fun showDateRangePickerDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_date_range_picker, null)
        val btnStartDate = dialogView.findViewById<Button>(R.id.btnStartDate)
        val btnEndDate = dialogView.findViewById<Button>(R.id.btnEndDate)
        val btnClear = dialogView.findViewById<Button>(R.id.btnClear)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnApply = dialogView.findViewById<Button>(R.id.btnApply)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Temporary variables to hold selections until Apply is clicked
        var tempStart = startDate
        var tempEnd = endDate

        // Update button text if values already exist
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
        // Listen for real-time updates to the events collection
        listenerRegistration = repository.listenToOpenEvents(
            { events -> 
                adapter.submitList(events)
                // Re-apply filter if there's text in the search bar
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
