package com.example.spiral_event_lottery_app

import com.example.spiral_event_lottery_app.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the User model class.
 */
class UserTest {

    @Test
    fun testUserCreation() {
        val user = User(
            deviceId = "dev_123",
            name = "John Doe",
            email = "john@example.com",
            phoneNumber = "1234567890",
            isAdmin = false
        )

        assertEquals("dev_123", user.deviceId)
        assertEquals("John Doe", user.name)
        assertEquals("john@example.com", user.email)
        assertEquals("1234567890", user.phoneNumber)
        assertEquals(false, user.isAdmin)
    }

    @Test
    fun testUserUpdate() {
        val user = User()
        user.name = "Jane Doe"
        user.email = "jane@example.com"
        user.isAdmin = true
        
        assertEquals("Jane Doe", user.name)
        assertEquals("jane@example.com", user.email)
        assertEquals(true, user.isAdmin)
    }

    @Test
    fun testEventList() {
        val user = User()
        user.eventList.add("event_1")
        user.eventList.add("event_2")
        
        assertEquals(2, user.eventList.size)
        assertTrue(user.eventList.contains("event_1"))
    }
}
