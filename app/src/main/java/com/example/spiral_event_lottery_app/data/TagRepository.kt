package com.example.spiral_event_lottery_app.data

import com.example.spiral_event_lottery_app.model.Tag
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class TagRepository {
    private val db = FirebaseFirestore.getInstance()
    private val tagsCollection = db.collection("tags")

    // Keep this as a fallback for offline/initial load
    private val defaultTags = mapOf(
        "Baseball" to listOf("Sports", "Family"),
        "Swimming" to listOf("Aquatics", "Sports"),
        "Waterpolo" to listOf("Aquatics", "Sports"),
        "Piano" to listOf("Music", "Education"),
        "Dance" to listOf("Performance", "Wellness"),
        "Painting" to listOf("Arts", "Hobbies"),
        "Yoga" to listOf("Wellness", "Sports"),
        "Coding" to listOf("Tech", "Education"),
        "Hiking" to listOf("Outdoors", "Wellness"),
        "Potluck" to listOf("Social", "Culinary"),
        "Networking" to listOf("Career", "Social"),
        "Storytime" to listOf("Family", "Education"),
        "Baking" to listOf("Culinary", "Hobbies"),
        "Robotics" to listOf("Science", "Tech"),
        "Photography" to listOf("Arts", "Tech"),
        "Cardio" to listOf("Wellness", "Sports"),
        "Pottery" to listOf("Arts", "Hobbies"),
        "Tutoring" to listOf("Education", "Career"),
        "Camping" to listOf("Outdoors", "Hobbies"),
        "Gardening" to listOf("Outdoors", "Wellness"),
        "Sports" to emptyList(),
        "Family" to emptyList(),
        "Aquatics" to emptyList(),
        "Music" to emptyList(),
        "Education" to emptyList(),
        "Performance" to emptyList(),
        "Wellness" to emptyList(),
        "Arts" to emptyList(),
        "Hobbies" to emptyList(),
        "Tech" to emptyList(),
        "Outdoors" to emptyList(),
        "Social" to emptyList(),
        "Culinary" to emptyList(),
        "Career" to emptyList(),
        "Science" to emptyList()
    )

    fun getTagImmediate(tagId: String): Tag {
        val parents = defaultTags[tagId] ?: emptyList()
        return Tag(id = tagId, parents = parents)
    }

    /**
     * Fetch the master list of categories for the UI from Firestore metadata.
     */
    suspend fun getParentCategories(): List<String> {
        return try {
            val doc = db.collection("metadata").document("categories").get().await()
            val categories = doc.get("list") as? List<String>

            // This is your "Safety Net" - make it match your Cloud Function list
            categories ?: listOf(
                "Sports", "Aquatics", "Outdoors", "Wellness", "Health", "Fitness",
                "Arts", "Music", "Performance", "Photography", "Fashion",
                "Education", "Tech", "Science", "Finance", "Career", "Business",
                "Family", "Social", "Culinary", "Community", "Gaming", "Travel", "Pets", "Networking"
            )
        } catch (e: Exception) {
            // Emergency fallback if the network is completely down
            listOf("Sports", "Music", "Tech", "Arts", "Wellness", "Education", "Social")
        }
    }

    /**
     * Fetches a tag from Firestore. If it doesn't exist, it checks local defaults,
     * saves it to Firestore (triggering AI categorization), and returns it.
     */
    suspend fun getTag(tagId: String): Tag? {
        return try {
            val doc = tagsCollection.document(tagId).get().await()
            if (doc.exists()) {
                doc.toObject(Tag::class.java)
            } else {
                // If it doesn't exist in DB, check local defaults or create new
                val parents = defaultTags[tagId] ?: emptyList()
                val newTag = Tag(id = tagId, parents = parents)
                
                // TRIGGER: Save it so Gemini can categorize it!
                saveTag(newTag)
                newTag
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Saves a tag to Firestore. Uses merge to avoid overwriting existing categorization.
     */
    suspend fun saveTag(tag: Tag) {
        try {
            tagsCollection.document(tag.id)
                .set(tag, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAllTags(): List<Tag> {
        return try {
            val snapshot = tagsCollection.get().await()
            val dbTags = snapshot.toObjects(Tag::class.java).associateBy { it.id }
            
            val allTags = mutableListOf<Tag>()
            allTags.addAll(dbTags.values)
            
            for ((id, parents) in defaultTags) {
                if (!dbTags.containsKey(id)) {
                    allTags.add(Tag(id = id, parents = parents))
                }
            }
            allTags
        } catch (e: Exception) {
            defaultTags.map { Tag(id = it.key, parents = it.value) }
        }
    }
}
