# Spiral Events

Spiral Events is an Android application designed to manage event lotteries, developed as a course assignment for **CMPUT 301**. The system facilitates a fair selection process for event participants when demand exceeds capacity, providing specialized roles for Entrants, Organizers, and Administrators.

## Features

### Entrant
- **Profile Management**: Create and update personal profiles, including name, email, and optional profile pictures (automatically generated if not provided).
- **Event Discovery**: Join event waitlists by scanning QR codes or browsing public events.
- **Lottery System**: Receive notifications when selected for an event. Entrants can choose to accept or decline invitations.
- **Geolocation**: Opt-in to provide location when joining a waitlist for events that require it.
- **Notifications**: Stay updated with real-time alerts regarding lottery results and event changes.

### Organizer
- **Event Creation**: Manage event details such as descriptions, posters, and participant limits.
- **Waitlist Management**: View and manage entrants on the waitlist. Map views available to see entrant locations if geolocation is enabled.
- **Lottery Control**: Trigger the random selection process to choose participants from the waitlist.
- **Redraw Functionality**: Automatically redraw new participants if selected entrants decline their invitations.
- **Communication**: Send targeted notifications to all waitlisted entrants, selected participants, or those who declined.

### Administrator
- **Content Management**: Monitor and remove profiles, events, and images that violate policies.
- **System Oversight**: Overview of all active events and registered users within the system.

## Technologies Used
- **Language**: Java & Kotlin
- **Database**: Firebase Firestore
- **Storage**: Firebase Storage
- **Authentication**: Device ID-based identification
- **Maps**: Google Maps API & Play Services Location
- **QR Codes**: ZXing & ML Kit Barcode Scanning
- **Image Loading**: Glide

## Contributors
This project was completed by a team of 7 members. We would like to credit everyone for their hard work and collaboration:

- koyinsola18
- sean12091
- Olofinti
- pybye12
- mchiew888
- temii70
- judxyz

## Getting Started
1. Clone the repository.
2. Open the project in Android Studio.
3. Ensure you have a `google-services.json` file in the `app/` directory (required for Firebase services).
4. Build and run the project on an Android emulator or physical device (API Level 24+ recommended).
