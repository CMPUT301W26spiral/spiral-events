package com.example.spiral_event_lottery_app.data

import com.example.spiral_event_lottery_app.model.Tag
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TagRepository {
    private val db = FirebaseFirestore.getInstance()
    private val tagsCollection = db.collection("tags")

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

    suspend fun getTag(tagId: String): Tag? {
        return try {
            val doc = tagsCollection.document(tagId).get().await()
            if (doc.exists()) {
                doc.toObject(Tag::class.java)
            } else {
                defaultTags[tagId]?.let { parents ->
                    Tag(id = tagId, parents = parents)
                } ?: Tag(id = tagId)
            }
        } catch (e: Exception) {
            null
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
