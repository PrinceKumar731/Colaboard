package com.colaboard.dto;

public class UserProfile {
    private String id;
    private String email;
    private String displayName;
    private String authProvider;

    public UserProfile() {
    }

    public UserProfile(String id, String email, String displayName, String authProvider) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.authProvider = authProvider;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }
}
