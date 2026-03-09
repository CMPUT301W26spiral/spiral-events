package com.example.spiral_event_lottery_app;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.title.setText(notification.getTitle());
        holder.message.setText(notification.getMessage());
        holder.date.setText(notification.getFormattedDate());

        // Set background color and button visibility based on type
        switch (notification.getType()) {
            case "ACCEPTED":
                holder.background.setBackgroundColor(Color.parseColor("#E8F5E9")); // Light Green
                holder.goButton.setVisibility(View.VISIBLE);
                break;
            case "DENIED":
                holder.background.setBackgroundColor(Color.parseColor("#FFEBEE")); // Light Red
                holder.goButton.setVisibility(View.VISIBLE);
                break;
            case "REQUESTED":
                holder.background.setBackgroundColor(Color.parseColor("#FFFDE7")); // Light Yellow
                holder.goButton.setVisibility(View.GONE);
                break;
            case "ORGANIZER":
                holder.background.setBackgroundColor(Color.parseColor("#F3E5F5")); // Light Purple
                holder.goButton.setVisibility(View.VISIBLE);
                break;
        }
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
