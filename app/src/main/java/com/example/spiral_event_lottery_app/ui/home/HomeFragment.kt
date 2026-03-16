package com.example.spiral_event_lottery_app.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spiral_event_lottery_app.R
import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.ui.details.EventDetailsFragment
import com.example.spiral_event_lottery_app.data.DeviceIdProvider
import com.example.spiral_event_lottery_app.model.Event
import com.example.spiral_event_lottery_app.ui.oevent.EventDetailsOFragment
import com.google.firebase.firestore.ListenerRegistration
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.Locale

/**
 * HomeFragment displays a list of all open events.
 * It identifies if the current user is the organizer of an event to show different options.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventAdapter
    private lateinit var repository: EventRepository
    private lateinit var searchEditText: EditText
    private lateinit var scanButton: Button
    private var allEvents: List<Event> = emptyList()
    private var listenerRegistration: ListenerRegistration? = null

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_LONG).show()
        } else {
            handleScannedQrCode(result.contents)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventRepository(requireContext())
        recyclerView = view.findViewById(R.id.eventsRecyclerView)
        searchEditText = view.findViewById(R.id.searchEditText)
        scanButton = view.findViewById(R.id.scanButton)
        
        // Retrieve the current device ID to check against organizerId
        val deviceId = DeviceIdProvider.getDeviceId(requireContext())

        adapter = EventAdapter(
            events = emptyList(),
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

        setupSearch()

        scanButton.setOnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Scan an Event QR Code")
            options.setCameraId(0) // Use a specific camera of the device
            options.setBeepEnabled(false)
            options.setBarcodeImageEnabled(true)
            barcodeLauncher.launch(options)
        }
    }

    private fun handleScannedQrCode(contents: String) {
        try {
            val uri = Uri.parse(contents)
            if (uri.scheme == "spiral-events" && uri.host == "event") {
                // If it's our deep link, let the Activity handle it via Intent
                val intent = Intent(Intent.ACTION_VIEW, uri)
                requireActivity().startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Invalid QR Code format", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error parsing QR Code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterEvents(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterEvents(query: String) {
        val filteredList = if (query.isBlank()) {
            allEvents
        } else {
            val normalizedQuery = query.trim().lowercase(Locale.getDefault())
            allEvents.map { event ->
                val name = event.name.lowercase(Locale.getDefault())
                val location = event.locationName.lowercase(Locale.getDefault())
                
                // Calculate similarity score (0.0 to 1.0)
                // We prioritize name similarity but also consider location
                val nameScore = fuzzyScore(normalizedQuery, name)
                val locationScore = fuzzyScore(normalizedQuery, location)
                
                val maxScore = maxOf(nameScore, locationScore * 0.8) // Location weighted slightly less
                Pair(event, maxScore)
            }
            .filter { it.second > 0.25 } // Threshold to filter out irrelevant results
            .sortedByDescending { it.second } // Sort by most similar
            .map { it.first }
        }
        adapter.submitList(filteredList)
    }

    /**
     * Simple fuzzy matching score.
     * Returns a value between 0.0 (no match) and 1.0 (exact match).
     */
    private fun fuzzyScore(query: String, target: String): Double {
        if (query == target) return 1.0
        if (target.contains(query)) {
            // Higher score if the target starts with the query
            return if (target.startsWith(query)) 0.9 else 0.7
        }
        
        val distance = levenshteinDistance(query, target)
        val maxLength = maxOf(query.length, target.length)
        if (maxLength == 0) return 0.0
        
        return 1.0 - (distance.toDouble() / maxLength.toDouble())
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    override fun onStart() {
        super.onStart()
        // Listen for real-time updates to the events collection
        listenerRegistration = repository.listenToOpenEvents(
            { events -> 
                allEvents = events
                filterEvents(searchEditText.text.toString())
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
