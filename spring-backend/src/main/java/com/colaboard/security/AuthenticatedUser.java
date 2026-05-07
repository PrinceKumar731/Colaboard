package com.colaboard.security;

import com.colaboard.dto.UserProfile;

import java.security.Principal;

public class AuthenticatedUser implements Principal {
    private final String id;
    private final String email;
    private final String displayName;
    private final String authProvider;

    public AuthenticatedUser(String id, String email, String displayName, String authProvider) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.authProvider = authProvider;
    }

    @Override
    public String getName() {
        return id;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public UserProfile toProfile() {
        return new UserProfile(id, email, displayName, authProvider);
    }
}
