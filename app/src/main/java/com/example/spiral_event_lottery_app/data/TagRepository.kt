package com.example.spiral_event_lottery_app.data

import android.util.Log
import com.example.spiral_event_lottery_app.model.Tag
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class TagRepository {
    private val db = FirebaseFirestore.getInstance()
    private val tagsCollection = db.collection("tags")
    private val TAG = "TagRepository"

    companion object {
        private val cachedTags = mutableMapOf<String, Tag>()
    }

    // Fallback defaults
    private val defaultTags = mapOf(
        "Baking" to listOf("Culinary", "Hobbies"),
        "Photography" to listOf("Arts", "Tech"),
        "Gaming" to listOf("Social", "Hobbies")
    )

    fun getTagImmediate(tagId: String): Tag {
        return cachedTags[tagId] ?: Tag(id = tagId, name = tagId, parents = defaultTags[tagId] ?: emptyList())
    }

    /**
     * Pre-fetches a list of tags and caches them for immediate use in sorting.
     */
    suspend fun fetchAndCacheTags(tagIds: List<String>) {
        val tagsToFetch = tagIds.filter { !cachedTags.containsKey(it) }.distinct()
        if (tagsToFetch.isEmpty()) return

        try {
            // Firestore 'in' query has a limit of 30 items. 
            // If tagIds is large, we might need to chunk it, but for a single screen it's usually fine.
            val chunks = tagsToFetch.chunked(30)
            for (chunk in chunks) {
                val snapshot = tagsCollection.whereIn("id", chunk).get().await()
                for (doc in snapshot.documents) {
                    val tag = doc.toObject(Tag::class.java)
                    if (tag != null) {
                        cachedTags[tag.id] = tag
                    }
                }
            }
            // For tags not found in DB, we still mark them as checked (cached with empty parents or defaults)
            // to avoid repeated network attempts.
            for (id in tagsToFetch) {
                if (!cachedTags.containsKey(id)) {
                    cachedTags[id] = Tag(id = id, name = id, parents = defaultTags[id] ?: emptyList())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tags: ${e.message}")
        }
    }

    suspend fun getParentCategories(): List<String> {
        val fallback = listOf(
            "Sports", "Aquatics", "Music", "Performance", "Arts", "Wellness",
            "Education", "Tech", "Outdoors", "Social", "Career", "Family",
            "Culinary", "Gaming", "Travel", "Finance", "Community", "Health",
            "Pets", "Science", "Hobbies"
        )
        return try {
            val doc = db.collection("metadata").document("categories").get().await()
            val categories = doc.get("list") as? List<String>
            categories ?: fallback
        } catch (e: Exception) {
            fallback
        }
    }

    suspend fun getTag(tagId: String): Tag? {
        if (cachedTags.containsKey(tagId)) return cachedTags[tagId]
        
        return try {
            val doc = tagsCollection.document(tagId).get().await()
            if (doc.exists()) {
                val tag = doc.toObject(Tag::class.java)
                if (tag != null) cachedTags[tagId] = tag
                tag
            } else {
                val parents = defaultTags[tagId] ?: emptyList()
                val newTag = Tag(id = tagId, name = tagId, parents = parents)
                saveTag(newTag)
                cachedTags[tagId] = newTag
                newTag
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveTag(tag: Tag) {
        try {
            Log.d(TAG, "DEBUG: Attempting to save tag: ${tag.id}")
            tagsCollection.document(tag.id)
                .set(tag, SetOptions.merge())
                .await()
            cachedTags[tag.id] = tag
            Log.d(TAG, "DEBUG: Successfully saved tag: ${tag.id}")
        } catch (e: Exception) {
            Log.e(TAG, "DEBUG: Failed to save tag: ${e.message}")
            e.printStackTrace()
        }
    }

    fun saveTagAsync(tag: Tag) {
        Log.d(TAG, "DEBUG: Attempting to save tag (Async): ${tag.id}")
        tagsCollection.document(tag.id)
            .set(tag, SetOptions.merge())
            .addOnSuccessListener { 
                cachedTags[tag.id] = tag
                Log.d(TAG, "DEBUG: Successfully saved tag (Async): ${tag.id}") 
            }
            .addOnFailureListener { e -> Log.e(TAG, "DEBUG: Failed to save tag (Async): ${e.message}") }
    }
}
