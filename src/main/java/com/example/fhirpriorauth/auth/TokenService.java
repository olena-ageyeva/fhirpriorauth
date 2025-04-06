package com.example.fhirpriorauth.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class TokenService {

    @Value("${availity.oauth.token-url}")
    private String tokenUrl;

    @Value("${availity.client-id}")
    private String clientId;

    @Value("${availity.client-secret}")
    private String clientSecret;

    @Value("${availity.oauth.scope}")
    private String scope;

    private final ObjectMapper mapper = new ObjectMapper();

    // Token cache
    private String cachedToken = null;
    private Instant tokenExpiration = null;

    // Buffer time before token expiration (in seconds)
    private static final int TOKEN_EXPIRY_BUFFER = 60; // 1 minute buffer
    /**
     * Fetch an access token from Availity
     *
     * @return The access token, or null if the token could not be obtained
     */
    public String fetchAccessToken() {
        // Check if we have a cached token that's still valid
        if (cachedToken != null && tokenExpiration != null && Instant.now().isBefore(tokenExpiration)) {
            System.out.println("🔑 Using cached access token (expires in " +
                    java.time.Duration.between(Instant.now(), tokenExpiration).getSeconds() + " seconds)");
            return cachedToken;
        }

        try {
            System.out.println("🔑 Fetching new access token from Availity...");
            System.out.println("   URL: " + tokenUrl);
            System.out.println("   Client ID: " + clientId);

            String requestBody = "grant_type=client_credentials"
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            System.out.println("   Sending request to token endpoint...");
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("❌ AUTHENTICATION ERROR: Failed to get token from Availity");
                System.out.println("   Status code: " + response.statusCode());
                System.out.println("   Response: " + response.body());
                System.out.println("   URL: " + tokenUrl);
                return null;
            }

            JsonNode node = mapper.readTree(response.body());
            String token = node.get("access_token").asText();
            int expiresIn = node.has("expires_in") ? node.get("expires_in").asInt() : 300;

            // Cache the token with expiration time (minus buffer)
            cachedToken = token;
            tokenExpiration = Instant.now().plusSeconds(expiresIn - TOKEN_EXPIRY_BUFFER);

            System.out.println("✅ AUTHENTICATION SUCCESS: Successfully obtained token from Availity");
            System.out.println("   Token: " + token.substring(0, 5) + "..." + token.substring(token.length() - 5));
            System.out.println("   Expires in: " + expiresIn + " seconds");
            System.out.println("   Cached until: " + tokenExpiration);

            return token;

        } catch (Exception e) {
            System.out.println("❌ AUTHENTICATION ERROR: Exception while fetching token");
            System.out.println("   Error: " + e.getMessage());
            System.out.println("   URL: " + tokenUrl);
            e.printStackTrace();
            return null;
        }
    }
}
