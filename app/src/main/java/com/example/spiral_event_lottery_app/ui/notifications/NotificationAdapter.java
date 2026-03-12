package com.example.spiral_event_lottery_app.ui.notifications;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spiral_event_lottery_app.R;
import com.example.spiral_event_lottery_app.model.Notification;
import com.example.spiral_event_lottery_app.ui.details.EventDetailsFragment;
import com.example.spiral_event_lottery_app.ui.details.EventDetailsLeaveFragment;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notificationList;

    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

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

        // Visibility and Navigation Logic
        switch (notification.getType()) {
            case "ACCEPTED":
                holder.title.setTextColor(Color.parseColor("#2E5A27"));
                holder.goButton.setVisibility(View.VISIBLE);
                break;
            case "DENIED":
                holder.title.setTextColor(Color.parseColor("#B71C1C"));
                holder.goButton.setVisibility(View.GONE); // FIXED: Removed from Denied
                break;
            case "REQUESTED":
                holder.title.setTextColor(Color.parseColor("#FF8F00"));
                holder.goButton.setVisibility(View.VISIBLE); // FIXED: Added to Requested
                break;
            case "ORGANIZER":
                holder.title.setTextColor(Color.parseColor("#6A1B9A"));
                holder.goButton.setVisibility(View.VISIBLE);
                break;
            default:
                holder.title.setTextColor(Color.BLACK);
                break;
        }

        // NAVIGATION: Go to details page when clicking "Go"
        holder.goButton.setOnClickListener(v -> {
            if (notification.getEventId() != null) {
                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                if (notification.getType().equals("REQUESTED") || notification.getType().equals("ACCEPTED")) {
                    // If they are joined, show the "Leave" version of details
                    activity.getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragmentContainer, EventDetailsLeaveFragment.newInstance(notification.getEventId()), "details_screen")
                        .addToBackStack("details")
                        .commit();
                } else {
                    // Otherwise show regular details
                    activity.getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragmentContainer, EventDetailsFragment.newInstance(notification.getEventId()), "details_screen")
                        .addToBackStack("details")
                        .commit();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, date;
        LinearLayout background;
        Button goButton;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            message = itemView.findViewById(R.id.notification_message);
            date = itemView.findViewById(R.id.notification_date);
            background = itemView.findViewById(R.id.notification_background);
            goButton = itemView.findViewById(R.id.notification_go_button);
            deleteButton = itemView.findViewById(R.id.notification_delete);
        }
    }
}
