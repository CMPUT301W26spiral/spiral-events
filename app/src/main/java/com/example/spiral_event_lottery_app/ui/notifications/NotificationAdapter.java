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

        switch (notification.getType()) {
            case "ACCEPTED":
                holder.title.setTextColor(Color.parseColor("#2E5A27"));
                holder.goButton.setVisibility(View.VISIBLE);
                break;
            case "DENIED":
                holder.title.setTextColor(Color.parseColor("#B71C1C"));
                holder.goButton.setVisibility(View.GONE); 
                break;
            case "REQUESTED":
                holder.title.setTextColor(Color.parseColor("#FF8F00"));
                holder.goButton.setVisibility(View.VISIBLE); 
                break;
            case "ORGANIZER":
                holder.title.setTextColor(Color.parseColor("#6A1B9A"));
                holder.goButton.setVisibility(View.VISIBLE);
                break;
            default:
                holder.title.setTextColor(Color.BLACK);
                break;
        }

        holder.goButton.setOnClickListener(v -> {
            String eventId = notification.getEventId();
            if (eventId != null) {
                Context context = v.getContext();
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
