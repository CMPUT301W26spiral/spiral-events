package com.example.spiral_event_lottery_app.ui.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spiral_event_lottery_app.R

/**
 * Adapter for displaying a list of event posters in a RecyclerView.
 * 
 * @property posterUrls A list of strings containing the URLs or URIs of the posters to be displayed.
 */
class PosterAdapter(private val posterUrls: List<String>) : RecyclerView.Adapter<PosterAdapter.PosterViewHolder>() {

    /**
     * Called when RecyclerView needs a new [PosterViewHolder] to represent an item.
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new PosterViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_poster, parent, false)
        return PosterViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * @param holder The ViewHolder which should be updated to represent the contents of the item.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
        holder.bind(posterUrls[position])
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = posterUrls.size

    /**
     * ViewHolder for poster items, responsible for binding image data to the [ImageView].
     */
    class PosterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_poster)

        /**
         * Binds a poster URL to the [ImageView] using Glide.
         * @param url The URL or URI string of the image to load.
         */
        fun bind(url: String) {
            Glide.with(itemView.context)
                .load(url)
                .placeholder(R.drawable.ic_event)
                .into(imageView)
        }
    }
}
