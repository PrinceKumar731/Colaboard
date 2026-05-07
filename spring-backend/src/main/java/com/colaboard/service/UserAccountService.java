package com.colaboard.service;

import com.colaboard.model.UserAccount;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserAccountService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, PasswordEncoder passwordEncoder) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount signup(String email, String password, String displayName) {
        String normalizedEmail = normalizeEmail(email);
        if (redisTemplate.hasKey(userEmailKey(normalizedEmail))) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(normalizedEmail);
        user.setDisplayName(displayName == null || displayName.isBlank() ? deriveDisplayName(normalizedEmail) : displayName.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAuthProvider("LOCAL");
        user.setCreatedAt(Instant.now());

        storeUser(user);
        redisTemplate.opsForValue().set(userEmailKey(normalizedEmail), user.getId());
        return user;
    }

    public UserAccount authenticate(String email, String password) {
        UserAccount user = findByEmail(email);
        if (user == null || user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return user;
    }

    public UserAccount upsertGoogleUser(String email, String displayName) {
        String normalizedEmail = normalizeEmail(email);
        UserAccount existing = findByEmail(normalizedEmail);
        if (existing != null) {
            if (existing.getDisplayName() == null || existing.getDisplayName().isBlank()) {
                existing.setDisplayName(displayName);
                storeUser(existing);
            }
            return existing;
        }

        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(normalizedEmail);
        user.setDisplayName(displayName == null || displayName.isBlank() ? deriveDisplayName(normalizedEmail) : displayName.trim());
        user.setPasswordHash(null);
        user.setAuthProvider("GOOGLE");
        user.setCreatedAt(Instant.now());

        storeUser(user);
        redisTemplate.opsForValue().set(userEmailKey(normalizedEmail), user.getId());
        return user;
    }

    public UserAccount findById(String userId) {
        String rawUser = redisTemplate.opsForValue().get(userIdKey(userId));
        if (rawUser == null) return null;
        try {
            return objectMapper.readValue(rawUser, UserAccount.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse user account from Redis", exception);
        }
    }

    public UserAccount findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        String userId = redisTemplate.opsForValue().get(userEmailKey(normalizedEmail));
        return userId != null ? findById(userId) : null;
    }

    private void storeUser(UserAccount user) {
        try {
            redisTemplate.opsForValue().set(userIdKey(user.getId()), objectMapper.writeValueAsString(user));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to store user account in Redis", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String deriveDisplayName(String email) {
        int idx = email.indexOf('@');
        return idx > 0 ? email.substring(0, idx) : email;
    }

    private String userIdKey(String userId) {
        return "colaboard:user:id:" + userId;
    }

    private String userEmailKey(String email) {
        return "colaboard:user:email:" + email;
    }
}
