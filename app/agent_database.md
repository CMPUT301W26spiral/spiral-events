# Firebase Schema

## Collection: events

| Field Name         | Type    | Description |
|-------------------|--------|-------------|
| id                | string | Event document ID |
| name              | string | Event name |
| description       | string | Event description |
| organizerId       | string | Reference to user who created the event |
| eventDate         | string | Event date |
| eventStartTime    | string | Event start time |
| eventEndTime      | string | Event end time |
| drawDate          | string | Lottery draw date |
| drawStartTime     | string | Lottery draw start time |
| drawEndTime       | string | Lottery draw end time |
| eventCreated      | string | Event creation timestamp |
| timeText          | string | Formatted event time |
| locationName      | string | Event location name |
| geolocation       | string | Geolocation enabled flag |
| interests         | string | Event tags/interests |
| posterUriString   | string | Poster image URL |
| maxEntrants       | int64  | Maximum participants |
| waiting_count     | int64  | Standardized waitlist count |
| lottery_done      | boolean| Lottery completion status |

---

## Sub-collection: events/{eventId}/waitlist

| Field Name | Type      | Description |
|------------|-----------|-------------|
| device_id  | string    | Entrant ID |
| joined_at  | timestamp | Time user joined the waitlist |

---

## Sub-collection: events/{eventId}/selected_list

| Field Name  | Type  | Description |
|-------------|-------|-------------|
| status      | Accepted or rejected|
| device_id   | string    | Entrant ID |
| selected_at | int64 | Timestamp when user was selected in draw |

---

## Collection: notifications

| Field Name    | Type      | Description |
|---------------|-----------|-------------|
| eventId       | string    | Reference to event |
| eventName     | string    | Event name |
| recipientId   | string    | Reference to user |
| title         | string    | Notification title |
| message       | string    | Notification message |
| type          | string    | Notification type |
| timestamp     | timestamp | Creation timestamp |

---

## Collection: users

| Field Name  | Type           | Description |
|-------------|----------------|-------------|
| deviceId    | string         | Device identifier |
| email       | string         | User email |
| name        | string         | User name |
| phoneNumber | string         | User phone number |
| photoUrl    | string         | Profile image URL |
| isAdmin     | boolean        | Administrator flag |
| eventList   | array (string) | List of event IDs user is involved in |

---

## Relationships

- events.organizerId → users.deviceId
- notifications.recipientId → users.deviceId
- notifications.eventId → events.id
- events/{eventId}/waitlist/{deviceId} → users.deviceId
- events/{eventId}/selected_list/{deviceId} → users.deviceId
