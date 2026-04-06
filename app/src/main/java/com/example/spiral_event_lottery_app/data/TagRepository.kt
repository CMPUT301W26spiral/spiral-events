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

    // Fallback defaults for common tags to ensure UI responsiveness
    private val defaultTags = mapOf(
        "Baking" to listOf("Culinary", "Hobbies"),
        "Photography" to listOf("Arts", "Tech"),
        "Gaming" to listOf("Social", "Hobbies"),
        "Baseball" to listOf("Sports", "Outdoors"),
        "Cycling" to listOf("Sports", "Outdoors", "Health"),
        "Race" to listOf("Sports", "Social", "Community"),
        "Team" to listOf("Sports", "Social"),
        "Home run" to listOf("Sports", "Career")
    )

    /**
     * Returns a tag from the cache immediately. 
     * If not in cache, returns a dummy tag with default parents if known.
     */
    fun getTagImmediate(tagId: String): Tag {
        return cachedTags[tagId] ?: Tag(
            id = tagId, 
            name = tagId, 
            parents = defaultTags.entries.find { it.key.equals(tagId, ignoreCase = true) }?.value ?: emptyList()
        )
    }

    /**
     * Pre-fetches a list of tags and caches them for immediate use in sorting.
     */
    suspend fun fetchAndCacheTags(tagIds: List<String>) {
        val tagsToFetch = tagIds.filter { id -> 
            !cachedTags.keys.any { it.equals(id, ignoreCase = true) } 
        }.distinct()
        
        if (tagsToFetch.isEmpty()) return

        try {
            // Firestore 'in' query has a limit of 30 items.
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
            
            // For tags not found in DB, mark them as checked (cached with defaults)
            for (id in tagsToFetch) {
                if (!cachedTags.keys.any { it.equals(id, ignoreCase = true) }) {
                    val parents = defaultTags.entries.find { it.key.equals(id, ignoreCase = true) }?.value ?: emptyList()
                    cachedTags[id] = Tag(id = id, name = id, parents = parents)
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
        val existing = cachedTags.entries.find { it.key.equals(tagId, ignoreCase = true) }?.value
        if (existing != null) return existing
        
        return try {
            val doc = tagsCollection.document(tagId).get().await()
            if (doc.exists()) {
                val tag = doc.toObject(Tag::class.java)
                if (tag != null) {
                    cachedTags[tag.id] = tag
                    tag
                } else null
            } else {
                val parents = defaultTags.entries.find { it.key.equals(tagId, ignoreCase = true) }?.value ?: emptyList()
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
            tagsCollection.document(tag.id)
                .set(tag, SetOptions.merge())
                .await()
            cachedTags[tag.id] = tag
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save tag: ${e.message}")
        }
    }

    fun saveTagAsync(tag: Tag) {
        tagsCollection.document(tag.id)
            .set(tag, SetOptions.merge())
            .addOnSuccessListener { 
                cachedTags[tag.id] = tag
            }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to save tag: ${e.message}") }
    }
}
