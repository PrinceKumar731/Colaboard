package com.colaboard.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleIdentityService {
    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentityService(@Value("${app.google.client-id}") String clientId) {
        this.clientId = clientId;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(clientId == null || clientId.isBlank() ? Collections.emptyList() : Collections.singletonList(clientId))
                .build();
    }

    public GoogleUser verify(String credential) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("Google login is not configured. Set GOOGLE_CLIENT_ID.");
        }
        try {
            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new IllegalArgumentException("Invalid Google credential.");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            return new GoogleUser(payload.getEmail(), (String) payload.get("name"));
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Failed to verify Google credential.", exception);
        }
    }

    public record GoogleUser(String email, String displayName) {
    }
}
