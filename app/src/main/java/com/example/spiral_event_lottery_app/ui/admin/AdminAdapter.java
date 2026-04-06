package com.example.spiral_event_lottery_app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.spiral_event_lottery_app.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the Admin Panel.
 * Displays a generic list of Firestore documents (events, users, images, notifications, comments)
 * and provides a Remove button for each item.
 */
public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(DocumentSnapshot document, String mode, int position);
    }

    private List<DocumentSnapshot> items;
    private String currentMode;
    private final OnDeleteListener deleteListener;

    public AdminAdapter(List<DocumentSnapshot> items, String currentMode, OnDeleteListener deleteListener) {
        this.items = items;
        this.currentMode = currentMode;
        this.deleteListener = deleteListener;
    }

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
        holder.imagesContainer.setVisibility(View.GONE);
        holder.imagesContainer.removeAllViews();
        holder.deleteBtn.setVisibility(View.VISIBLE);
        holder.subtitle.setText("");

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
                holder.title.setText("Posters for: " + (doc.getString("name") != null ? doc.getString("name") : "Event"));
                
                // Get list of multiple posters
                List<String> posterUrls = (List<String>) doc.get("posterUriStrings");
                if (posterUrls == null) {
                    posterUrls = new ArrayList<>();
                    String singleUrl = doc.getString("posterUriString");
                    if (singleUrl != null) posterUrls.add(singleUrl);
                }

                if (!posterUrls.isEmpty()) {
                    holder.imagesContainer.setVisibility(View.VISIBLE);
                    for (String url : posterUrls) {
                        ImageView iv = new ImageView(holder.itemView.getContext());
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(250, 250);
                        params.setMargins(0, 0, 16, 0);
                        iv.setLayoutParams(params);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        
                        Glide.with(holder.itemView.getContext())
                                .load(url)
                                .placeholder(R.drawable.ic_event)
                                .into(iv);
                                
                        holder.imagesContainer.addView(iv);
                    }
                    holder.subtitle.setText(posterUrls.size() + " image(s) found");
                } else {
                    holder.subtitle.setText("No posters found");
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
                    FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                            .addOnSuccessListener(eventDoc -> {
                                if (eventDoc.exists()) {
                                    String eventName = eventDoc.getString("name");
                                    holder.subtitle.setText("Event: " + (eventName != null ? eventName : "Unnamed"));
                                }
                            });
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        Button deleteBtn;
        ImageView image;
        LinearLayout imagesContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_admin_title);
            subtitle = itemView.findViewById(R.id.tv_admin_subtitle);
            deleteBtn = itemView.findViewById(R.id.btn_admin_delete);
            image = itemView.findViewById(R.id.iv_admin_image);
            imagesContainer = itemView.findViewById(R.id.ll_admin_images_container);
        }
    }
}