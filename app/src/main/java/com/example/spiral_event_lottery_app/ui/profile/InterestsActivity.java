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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterestsActivity extends AppCompatActivity {

    private enum State {
        NEUTRAL, INTERESTED, NOT_INTERESTED
    }

    private final String[] INTEREST_NAMES = {
            "Sports", "Aquatics", "Music", "Performance", "Arts",
            "Wellness", "Education", "Tech", "Outdoors", "Social",
            "Career", "Family", "Culinary", "Science", "Hobbies"
    };

    private FirebaseFirestore db;
    private String uid;
    private FlexboxLayout flexbox;
    private final Map<String, State> interestStates = new HashMap<>();
    private final Map<String, TextView> interestViews = new HashMap<>();

    // We need to know if the last neutral state was before or after "Interested" 
    // to implement the requested sequence: Neutral -> Interested -> Neutral -> Not Interested -> Neutral
    private final Map<String, Boolean> wasInterestedBeforeNeutral = new HashMap<>();

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
            wasInterestedBeforeNeutral.put(name, false);
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
        tv.setTextColor(Color.BLACK);
        
        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(12, 12, 12, 12);
        tv.setLayoutParams(params);
        
        updateViewStyle(tv, State.NEUTRAL);

        tv.setOnClickListener(v -> {
            State currentState = interestStates.get(name);
            State nextState;
            
            if (currentState == State.NEUTRAL) {
                if (wasInterestedBeforeNeutral.get(name)) {
                    nextState = State.NOT_INTERESTED;
                } else {
                    nextState = State.INTERESTED;
                }
            } else if (currentState == State.INTERESTED) {
                nextState = State.NEUTRAL;
                wasInterestedBeforeNeutral.put(name, true);
            } else { // currentState == State.NOT_INTERESTED
                nextState = State.NEUTRAL;
                wasInterestedBeforeNeutral.put(name, false);
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
                break;
            case NOT_INTERESTED:
                tv.setBackgroundResource(R.drawable.interest_not_interested_bg);
                break;
            case NEUTRAL:
            default:
                tv.setBackgroundResource(R.drawable.interest_neutral_bg);
                break;
        }
    }

    private void loadInterestsFromFirebase() {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                List<String> interested = (List<String>) doc.get("interested");
                List<String> notInterested = (List<String>) doc.get("notInterested");

                if (interested != null) {
                    for (String s : interested) {
                        if (interestStates.containsKey(s)) {
                            interestStates.put(s, State.INTERESTED);
                            updateViewStyle(interestViews.get(s), State.INTERESTED);
                        }
                    }
                }
                if (notInterested != null) {
                    for (String s : notInterested) {
                        if (interestStates.containsKey(s)) {
                            interestStates.put(s, State.NOT_INTERESTED);
                            updateViewStyle(interestViews.get(s), State.NOT_INTERESTED);
                        }
                    }
                }
            }
        });
    }

    private void saveInterestsToFirebase() {
        List<String> interested = new ArrayList<>();
        List<String> notInterested = new ArrayList<>();

        for (Map.Entry<String, State> entry : interestStates.entrySet()) {
            if (entry.getValue() == State.INTERESTED) {
                interested.add(entry.getKey());
            } else if (entry.getValue() == State.NOT_INTERESTED) {
                notInterested.add(entry.getKey());
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("interested", interested);
        data.put("notInterested", notInterested);

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
