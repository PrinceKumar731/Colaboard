package com.colaboard.controller;

import com.colaboard.dto.AuthRequest;
import com.colaboard.dto.AuthResponse;
import com.colaboard.dto.GoogleAuthRequest;
import com.colaboard.model.UserAccount;
import com.colaboard.security.AuthenticatedUser;
import com.colaboard.service.GoogleIdentityService;
import com.colaboard.service.JwtService;
import com.colaboard.service.UserAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountService userAccountService;
    private final JwtService jwtService;
    private final GoogleIdentityService googleIdentityService;

    public AuthController(UserAccountService userAccountService, JwtService jwtService, GoogleIdentityService googleIdentityService) {
        this.userAccountService = userAccountService;
        this.jwtService = jwtService;
        this.googleIdentityService = googleIdentityService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@RequestBody AuthRequest request) {
        validateLocalAuthRequest(request, true);
        UserAccount user = userAccountService.signup(request.getEmail(), request.getPassword(), request.getDisplayName());
        return new AuthResponse(jwtService.generateToken(user), toPrincipal(user).toProfile());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        validateLocalAuthRequest(request, false);
        UserAccount user = userAccountService.authenticate(request.getEmail(), request.getPassword());
        return new AuthResponse(jwtService.generateToken(user), toPrincipal(user).toProfile());
    }

    @PostMapping("/google")
    public AuthResponse google(@RequestBody GoogleAuthRequest request) {
        if (request.getCredential() == null || request.getCredential().isBlank()) {
            throw new IllegalArgumentException("Google credential is required.");
        }
        GoogleIdentityService.GoogleUser googleUser = googleIdentityService.verify(request.getCredential());
        UserAccount user = userAccountService.upsertGoogleUser(googleUser.email(), googleUser.displayName());
        return new AuthResponse(jwtService.generateToken(user), toPrincipal(user).toProfile());
    }

    @GetMapping("/me")
    public Object me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalArgumentException("User is not authenticated.");
        }
        return user.toProfile();
    }

    private void validateLocalAuthRequest(AuthRequest request, boolean requireDisplayName) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }
        if (requireDisplayName && (request.getDisplayName() == null || request.getDisplayName().isBlank())) {
            throw new IllegalArgumentException("Display name is required.");
        }
    }

    private AuthenticatedUser toPrincipal(UserAccount user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getDisplayName(), user.getAuthProvider());
    }
}
