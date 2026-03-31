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

    // Fallback defaults
    private val defaultTags = mapOf(
        "Baking" to listOf("Culinary", "Hobbies"),
        "Photography" to listOf("Arts", "Tech"),
        "Gaming" to listOf("Gaming", "Social")
    )

    fun getTagImmediate(tagId: String): Tag {
        val parents = defaultTags[tagId] ?: emptyList()
        return Tag(id = tagId, name = tagId, parents = parents)
    }

    suspend fun getParentCategories(): List<String> {
        return try {
            val doc = db.collection("metadata").document("categories").get().await()
            val categories = doc.get("list") as? List<String>
            categories ?: listOf("Sports", "Music", "Tech", "Arts", "Wellness", "Education", "Social")
        } catch (e: Exception) {
            listOf("Sports", "Music", "Tech", "Arts", "Wellness", "Education", "Social")
        }
    }

    suspend fun getTag(tagId: String): Tag? {
        return try {
            val doc = tagsCollection.document(tagId).get().await()
            if (doc.exists()) {
                doc.toObject(Tag::class.java)
            } else {
                val parents = defaultTags[tagId] ?: emptyList()
                val newTag = Tag(id = tagId, name = tagId, parents = parents)
                saveTag(newTag)
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
            .addOnSuccessListener { Log.d(TAG, "DEBUG: Successfully saved tag (Async): ${tag.id}") }
            .addOnFailureListener { e -> Log.e(TAG, "DEBUG: Failed to save tag (Async): ${e.message}") }
    }
}
