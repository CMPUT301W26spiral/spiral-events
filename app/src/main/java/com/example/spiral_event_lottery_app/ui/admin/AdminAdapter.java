package com.example.spiral_event_lottery_app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.spiral_event_lottery_app.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * RecyclerView adapter for the Admin Panel.
 * Displays a generic list of Firestore documents (events, users, images, notifications, comments)
 * and provides a Remove button for each item.
 *
 * Used by AdminFragment to fulfil US 03.01.01 through US 03.08.01 and US 03.10.01.
 *
 * @author Abdul Haq Bin Abdul Rehman
 */
public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.ViewHolder> {

    /** Callback interface for delete button clicks. */
    public interface OnDeleteListener {
        /**
         * Called when the Remove button is tapped.
         * @param document The Firestore document to be removed.
         * @param mode     Current display mode (e.g. "EVENTS", "PROFILES").
         * @param position Adapter position of the item.
         */
        void onDelete(DocumentSnapshot document, String mode, int position);
    }
    private List<DocumentSnapshot> items;
    private String currentMode;
    private final OnDeleteListener deleteListener;

    /**
     * Constructs a new AdminAdapter.
     *
     * @param items          Initial list of Firestore documents.
     * @param currentMode    Display mode string (EVENTS, PROFILES, IMAGES, NOTIFICATIONS, COMMENTS).
     * @param deleteListener Callback invoked when Remove is tapped.
     */
    public AdminAdapter(List<DocumentSnapshot> items, String currentMode, OnDeleteListener deleteListener) {
        this.items = items;
        this.currentMode = currentMode;
        this.deleteListener = deleteListener;
    }
    /**
     * Replaces the adapter's data set and refreshes the list.
     *
     * @param newItems New list of documents.
     * @param mode     New display mode.
     */
    public void updateData(List<DocumentSnapshot> newItems, String mode) {
        this.items = newItems;
        this.currentMode = mode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin, parent, false);
        return new ViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = items.get(position);

        // Reset views for recycling
        holder.image.setVisibility(View.GONE);
        holder.deleteBtn.setVisibility(View.VISIBLE);
        holder.subtitle.setText("");

        // we display different fields depending on the current browse mode
        switch (currentMode) {
            case "EVENTS":
                holder.title.setText(doc.getString("name") != null ? doc.getString("name") : "Unnamed Event");
                holder.subtitle.setText("ID: " + doc.getId());
                break;
            case "PROFILES":
                holder.title.setText(doc.getString("name") != null ? doc.getString("name") : "Unnamed User");
                holder.subtitle.setText("Device: " + doc.getId());
                break;
            case "IMAGES":
                holder.title.setText("Poster: " + (doc.getString("name") != null ? doc.getString("name") : "Event"));
                String imageUrl = doc.getString("posterUriString");
                holder.subtitle.setText(imageUrl != null ? imageUrl : "No URL");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    holder.image.setVisibility(View.VISIBLE);
                    Glide.with(holder.itemView.getContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_event)
                            .into(holder.image);
                }
                break;
            case "NOTIFICATIONS":
                holder.title.setText(doc.getString("title") != null ? doc.getString("title") : "Notification");
                holder.subtitle.setText("To: " + doc.getString("recipientId"));
                holder.deleteBtn.setVisibility(View.GONE);
                break;
            case "COMMENTS":
                String author = doc.getString("authorName");
                String text = doc.getString("text");
                holder.title.setText((author != null ? author : "User") + ": " + (text != null ? text : ""));
                
                String eventId = doc.getString("eventId");
                if (eventId != null) {
                    holder.subtitle.setText("Event ID: " + eventId);
                    // Attempt to fetch event name for better context
                    FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                            .addOnSuccessListener(eventDoc -> {
                                if (eventDoc.exists()) {
                                    String eventName = eventDoc.getString("name");
                                    holder.subtitle.setText("Event: " + (eventName != null ? eventName : "Unnamed"));
                                }
                            });
                } else {
                    holder.subtitle.setText("Unknown Event context");
                }
                break;
            default:
                holder.title.setText(doc.getId());
                holder.subtitle.setText(currentMode);
        }

        holder.deleteBtn.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(doc, currentMode, position);
        });
    }
    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    /** ViewHolder for a single admin list item. */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        Button deleteBtn;
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_admin_title);
            subtitle = itemView.findViewById(R.id.tv_admin_subtitle);
            deleteBtn = itemView.findViewById(R.id.btn_admin_delete);
            image = itemView.findViewById(R.id.iv_admin_image);
        }
    }
}