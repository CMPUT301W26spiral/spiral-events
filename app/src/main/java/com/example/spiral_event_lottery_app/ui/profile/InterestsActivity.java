package com.example.spiral_event_lottery_app.ui.profile;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spiral_event_lottery_app.R;
import com.example.spiral_event_lottery_app.data.DeviceIdProvider;
import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterestsActivity extends AppCompatActivity {

    private enum State {
        NEUTRAL, INTERESTED, NOT_INTERESTED
    }

    private final String[] INTEREST_NAMES = {
            "Sports", "Aquatics", "Music", "Performance", "Arts",
            "Wellness", "Education", "Tech", "Outdoors", "Social",
            "Career", "Family", "Culinary", "Science", "Hobbies"
    };
    private final Set<String> DEFAULT_INTERESTS = new HashSet<>(Arrays.asList(INTEREST_NAMES));

    private FirebaseFirestore db;
    private String uid;
    private FlexboxLayout flexbox;
    private final Map<String, State> interestStates = new HashMap<>();
    private final Map<String, TextView> interestViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests);

        db = FirebaseFirestore.getInstance();
        uid = DeviceIdProvider.getDeviceId(this);

        flexbox = findViewById(R.id.interestsFlexbox);
        Button saveButton = findViewById(R.id.saveInterestsButton);

        initializeInterests();
        loadInterestsFromFirebase();

        saveButton.setOnClickListener(v -> saveInterestsToFirebase());
    }

    private void initializeInterests() {
        for (String name : INTEREST_NAMES) {
            interestStates.put(name, State.NEUTRAL);
            TextView textView = createInterestView(name);
            interestViews.put(name, textView);
            flexbox.addView(textView);
        }
    }

    private TextView createInterestView(String name) {
        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setPadding(32, 16, 32, 16);
        tv.setTextSize(16);
        
        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(12, 12, 12, 12);
        tv.setLayoutParams(params);
        
        updateViewStyle(tv, interestStates.getOrDefault(name, State.NEUTRAL));

        tv.setOnClickListener(v -> {
            State currentState = interestStates.get(name);
            State nextState;
            
            if (currentState == State.NEUTRAL) {
                nextState = State.INTERESTED;
            } else if (currentState == State.INTERESTED) {
                nextState = State.NOT_INTERESTED;
            } else { // State.NOT_INTERESTED
                nextState = State.NEUTRAL;
            }
            
            interestStates.put(name, nextState);
            updateViewStyle(tv, nextState);
        });

        return tv;
    }

    private void updateViewStyle(TextView tv, State state) {
        switch (state) {
            case INTERESTED:
                tv.setBackgroundResource(R.drawable.interest_interested_bg);
                tv.setTextColor(Color.BLACK);
                break;
            case NOT_INTERESTED:
                tv.setBackgroundResource(R.drawable.interest_not_interested_bg);
                tv.setTextColor(Color.BLACK);
                break;
            case NEUTRAL:
            default:
                tv.setBackgroundResource(R.drawable.interest_neutral_bg);
                tv.setTextColor(Color.BLACK);
                break;
        }
    }

    private void loadInterestsFromFirebase() {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                List<String> interested = (List<String>) doc.get("interested");
                List<String> notInterested = (List<String>) doc.get("notInterested");
                List<String> customInterests = (List<String>) doc.get("customInterests");

                if (customInterests != null) {
                    for (String s : customInterests) {
                        addOrUpdateInterest(s, State.NEUTRAL);
                    }
                }

                if (interested != null) {
                    for (String s : interested) {
                        addOrUpdateInterest(s, State.INTERESTED);
                    }
                }
                if (notInterested != null) {
                    for (String s : notInterested) {
                        addOrUpdateInterest(s, State.NOT_INTERESTED);
                    }
                }
            }
        });
    }

    private void addOrUpdateInterest(String name, State state) {
        if (interestStates.containsKey(name)) {
            // Only update if the new state is NOT neutral, or if we want to force reset
            // In loading, we load neutral custom first, then overwrite with interested/notInterested
            if (state != State.NEUTRAL) {
                interestStates.put(name, state);
                updateViewStyle(interestViews.get(name), state);
            }
            return;
        }
        
        interestStates.put(name, state);
        TextView textView = createInterestView(name);
        interestViews.put(name, textView);
        updateViewStyle(textView, state);
        flexbox.addView(textView);
    }

    private void saveInterestsToFirebase() {
        List<String> interested = new ArrayList<>();
        List<String> notInterested = new ArrayList<>();
        List<String> customInterests = new ArrayList<>();

        for (Map.Entry<String, State> entry : interestStates.entrySet()) {
            String name = entry.getKey();
            State state = entry.getValue();
            
            if (state == State.INTERESTED) {
                interested.add(name);
            } else if (state == State.NOT_INTERESTED) {
                notInterested.add(name);
            }
            
            // Persist all non-default interests that are in our map (meaning they were loaded or touched)
            if (!DEFAULT_INTERESTS.contains(name)) {
                customInterests.add(name);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("interested", interested);
        data.put("notInterested", notInterested);
        data.put("customInterests", customInterests);

        db.collection("users").document(uid).update(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Interests saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    db.collection("users").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Interests saved", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                });
    }
}
