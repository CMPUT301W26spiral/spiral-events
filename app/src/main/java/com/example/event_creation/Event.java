package com.example.event_creation;

import java.io.Serializable;

/**
 * Model class representing an event in the system.
 * Contains details about the event location, timing, and lottery draw information.
 */
public class Event implements Serializable {
    private String id;
    private String name;
    private String location;
    private String interests;
    private String description;
    private String geolocation;
    private Integer maxEntrants;
    
    private String eventDate; // Format: DD/MM/YYYY
    private String eventStartTime; // Format: HH:MM
    private String eventEndTime; // Format: HH:MM
    
    private String drawDate; // Format: DD/MM/YYYY
    private String drawStartTime; // Format: HH:MM
    private String drawEndTime; // Format: HH:MM

    private String posterUriString; // Store the URI as a string for serialization
    private String qrCodeUrl;
    private String qrHash;

    /**
     * Default constructor required for Firebase operations.
     */
    public Event() {
    }

    /**
     * Constructs a new Event with all details including poster.
     */
    public Event(String name, String location, String interests, String description, String geolocation, Integer maxEntrants, String eventDate, String eventStartTime, String eventEndTime, String drawDate, String drawStartTime, String drawEndTime, String posterUriString) {
        this.name = name;
        this.location = location;
        this.interests = interests;
        this.description = description;
        this.geolocation = geolocation;
        this.maxEntrants = maxEntrants;
        this.eventDate = eventDate;
        this.eventStartTime = eventStartTime;
        this.eventEndTime = eventEndTime;
        this.drawDate = drawDate;
        this.drawStartTime = drawStartTime;
        this.drawEndTime = drawEndTime;
        this.posterUriString = posterUriString;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGeolocation() { return geolocation; }
    public void setGeolocation(String geolocation) { this.geolocation = geolocation; }

    public Integer getMaxEntrants() { return maxEntrants; }
    public void setMaxEntrants(Integer maxEntrants) { this.maxEntrants = maxEntrants; }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public String getEventStartTime() { return eventStartTime; }
    public void setEventStartTime(String eventStartTime) { this.eventStartTime = eventStartTime; }

    public String getEventEndTime() { return eventEndTime; }
    public void setEventEndTime(String eventEndTime) { this.eventEndTime = eventEndTime; }

    public String getDrawDate() { return drawDate; }
    public void setDrawDate(String drawDate) { this.drawDate = drawDate; }

    public String getDrawStartTime() { return drawStartTime; }
    public void setDrawStartTime(String drawStartTime) { this.drawStartTime = drawStartTime; }

    public String getDrawEndTime() { return drawEndTime; }
    public void setDrawEndTime(String drawEndTime) { this.drawEndTime = drawEndTime; }

    public String getPosterUriString() { return posterUriString; }
    public void setPosterUriString(String posterUriString) { this.posterUriString = posterUriString; }

    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }

    public String getQrHash() { return qrHash; }
    public void setQrHash(String qrHash) { this.qrHash = qrHash; }
}
