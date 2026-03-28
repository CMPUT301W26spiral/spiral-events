package com.example.spiral_event_lottery_app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.spiral_event_lottery_app.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * EntrantMapFragment displays a Google Map with markers showing
 * the geographical locations of all entrants who joined a specific event's waitlist.
 *
 * Fulfils US 02.02.02 – As an organizer I want to see on a map where
 * entrants joined my event waiting list from.
 *
 * @author Abdul Haq Bin Abdul Rehman
 */
public class EntrantMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_EVENT_ID = "event_id";
    private String eventId;
    private FirebaseFirestore db;

    /**
     * Factory method — creates an instance with the event ID bundled as an argument.
     *
     * @param eventId The Firestore document ID of the event.
     * @return A configured EntrantMapFragment.
     */
    public static EntrantMapFragment newInstance(String eventId) {
        EntrantMapFragment fragment = new EntrantMapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entrant_map, container, false);
        view.findViewById(R.id.map_back_btn).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Called when the map is ready. Fetches all waitlist documents for the event
     * and adds a map marker for each entrant that has latitude/longitude stored.
     *
     * @param googleMap The ready GoogleMap instance.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        if (eventId == null) {
            Toast.makeText(requireContext(), "No event ID provided", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events").document(eventId).collection("waitlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    LatLng lastLocation = null;
                    int markerCount = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        if (lat != null && lng != null) {
                            LatLng position = new LatLng(lat, lng);
                            googleMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title("Entrant joined here"));
                            lastLocation = position;
                            markerCount++;
                        }
                    }

                    if (lastLocation != null) {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 10f));
                    } else {
                        Toast.makeText(requireContext(),
                                "No location data — entrants joined without geolocation.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load locations", Toast.LENGTH_SHORT).show());
    }
}