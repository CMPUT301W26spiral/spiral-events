package com.example.spiral_event_lottery_app.ui.notifications;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spiral_event_lottery_app.R;
import com.example.spiral_event_lottery_app.data.EventRepository;
import com.example.spiral_event_lottery_app.model.Notification;
import com.example.spiral_event_lottery_app.ui.details.EventDetailsFragment;
import com.example.spiral_event_lottery_app.ui.details.EventDetailsLeaveFragment;

import java.util.List;

/**
 * Adapter for the RecyclerView in NotificationFragment.
 * Responsible for binding Notification data to the notification_item UI and handling user interactions.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notificationList;

    /**
     * Constructs a new NotificationAdapter.
     * @param notificationList The list of notifications to be displayed.
     */
    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds data to the UI elements of a single notification card.
     * Sets colors based on notification type and configures the "Go" button navigation.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        String displayTitle = notification.getEventName() != null ?
                notification.getTitle() + ": " + notification.getEventName() :
                notification.getTitle();

        holder.title.setText(displayTitle);
        holder.message.setText(notification.getMessage());
        holder.date.setText(notification.getFormattedDate());

        holder.background.setBackgroundColor(Color.WHITE);

        // UI Styling based on notification category
        switch (notification.getType()) {
            case "ACCEPTED":
                holder.title.setTextColor(Color.parseColor("#2E5A27")); // Green
                holder.goButton.setVisibility(View.VISIBLE);
                holder.goButton.setText("Accept/Decline"); // Let them know it's an action button
                break;
            case "DENIED":
                holder.title.setTextColor(Color.parseColor("#B71C1C")); // Red
                holder.goButton.setVisibility(View.GONE);
                break;
            case "REQUESTED":
                holder.title.setTextColor(Color.parseColor("#FF8F00")); // Amber
                holder.goButton.setVisibility(View.VISIBLE);
                break;
            case "ORGANIZER":
                holder.title.setTextColor(Color.parseColor("#6A1B9A")); // Purple
                holder.goButton.setVisibility(View.VISIBLE);
                break;
            default:
                holder.title.setTextColor(Color.BLACK);
                break;
        }

        // we Handle navigation OR Accept/Decline Dialog
        holder.goButton.setOnClickListener(v -> {
            String eventId = notification.getEventId();
            if (eventId != null) {
                Context context = v.getContext();

                // ACCEPTED is to show the choice dialog
                if ("ACCEPTED".equals(notification.getType())) {
                    new androidx.appcompat.app.AlertDialog.Builder(context)
                            .setTitle("Congratulations!")
                            .setMessage("You have been chosen for this event! Do you want to accept or decline the invitation?")
                            .setPositiveButton("Accept", (dialog, which) -> {
                                // Trigger the acceptanceHandling class
                                com.example.spiral_event_lottery_app.acceptanceHandling handler = 
                                    new com.example.spiral_event_lottery_app.acceptanceHandling();
                                handler.invitation_accepted(context, eventId, notification.getRecipientId());
                                holder.goButton.setText("Accepted");
                                holder.goButton.setEnabled(false);
                            })
                            .setNegativeButton("Decline", (dialog, which) -> {
                                com.example.spiral_event_lottery_app.acceptanceHandling handler = 
                                    new com.example.spiral_event_lottery_app.acceptanceHandling();
                                handler.invitation_declined(context, eventId, notification.getRecipientId());
                                holder.goButton.setText("Declined");
                                holder.goButton.setEnabled(false);
                            })
                        })
                            .show();
                    return; // Stop execution here so it doesn't navigate to the details fragment
                }

                AppCompatActivity activity = getActivity(context);
                if (activity == null) return;

                EventRepository repository = new EventRepository(activity);
                repository.isJoined(eventId, joined -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;

                    if (joined) {
                        activity.getSupportFragmentManager().beginTransaction()
                                .add(R.id.fragmentContainer, EventDetailsLeaveFragment.Companion.newInstance(eventId), "details_screen")
                                .addToBackStack("details")
                                .commit();
                    } else if (notification.getType().equals("DENIED") || notification.getType().equals("CANCELLED")) {
                        activity.getSupportFragmentManager().beginTransaction()
                                .add(R.id.fragmentContainer, EventDetailsFragment.Companion.newInstance(eventId), "details_screen")
                                .addToBackStack("details")
                                .commit();
                    } else {
                        Toast.makeText(activity, "You are no longer on the waiting list for this event.", Toast.LENGTH_SHORT).show();
                    }
                }, e -> {
                    Toast.makeText(activity, "Error checking event status", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Helper to safely unwrap Context to find the hosting Activity.
     */
    private AppCompatActivity getActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof AppCompatActivity) {
                return (AppCompatActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    /**
     * ViewHolder class for individual notification items.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, date;
        LinearLayout background;
        Button goButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            message = itemView.findViewById(R.id.notification_message);
            date = itemView.findViewById(R.id.notification_date);
            background = itemView.findViewById(R.id.notification_background);
            goButton = itemView.findViewById(R.id.notification_go_button);
        }
    }
}
