package com.example.fhirpriorauth.controller;

import com.example.fhirpriorauth.auth.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prior-auth/auth")
public class AuthTestController {

    private final TokenService tokenService;

    @Autowired
    public AuthTestController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping
    public String testAuth() {
        try {
            String token = tokenService.fetchAccessToken();

            if (token == null) {
                return "Authentication failed: Unable to obtain token from Availity. Please check your credentials.";
            }

            // Mask the token for security (show only first and last few characters)
            String maskedToken = maskToken(token);
            return "Authentication successful! Token: " + maskedToken;
        } catch (Exception e) {
            return "Authentication failed: " + e.getMessage();
        }
    }

    @GetMapping("/token")
    public String getAccessToken() {
        try {
            String token = tokenService.fetchAccessToken();
            // Mask the token for security (show only first and last few characters)
            String maskedToken = maskToken(token);
            return "Access Token: " + maskedToken;
        } catch (Exception e) {
            return "Failed to get token: " + e.getMessage();
        }
    }

    /**
     * Masks a token for display, showing only the first and last few characters
     */
    private String maskToken(String token) {
        // We should never get here with a null token since we check for it in the calling method
        if (token == null) {
            return "";
        }

        if (token.length() < 10) {
            return token; // Token is too short to mask, just return it
        }

        int firstChars = 5;
        int lastChars = 5;

        if (token.length() <= firstChars + lastChars) {
            return token; // Token is too short to mask
        }

        String firstPart = token.substring(0, firstChars);
        String lastPart = token.substring(token.length() - lastChars);

        return firstPart + "..." + lastPart;
    }
}
