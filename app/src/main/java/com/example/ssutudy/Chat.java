package com.example.ssutudy;

import com.google.firebase.Timestamp;

public class Chat {
    private String name;
    private String message;
    private String uid;
    private Timestamp timestamp;

    public Chat() {
        // Required for Firestore
    }

    public Chat(String name, String message, String uid) {
        this.name = name;
        this.message = message;
        this.uid = uid;
        this.timestamp = null; // Default, can be set later
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
