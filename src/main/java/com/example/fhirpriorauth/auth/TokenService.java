package com.example.fhirpriorauth.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

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
            log.info("Using cached access token (expires in {} seconds)",
                    java.time.Duration.between(Instant.now(), tokenExpiration).getSeconds());
            return cachedToken;
        }

        try {
            log.info("Fetching new access token from Availity");
            log.debug("Token URL: {}", tokenUrl);
            log.debug("Client ID: {}", clientId);
            log.debug("Scope: {}", scope);

            String requestBody = "grant_type=client_credentials"
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            log.debug("Sending request to token endpoint");
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("Received response with status code: {}", response.statusCode());

            if (response.statusCode() != 200) {
                log.error("AUTHENTICATION ERROR: Failed to get token from Availity");
                log.error("Status code: {}", response.statusCode());
                log.error("Response: {}", response.body());
                log.error("URL: {}", tokenUrl);
                return null;
            }

            JsonNode node = mapper.readTree(response.body());

            // Check if the response contains an access token
            if (!node.has("access_token")) {
                log.error("Response does not contain an access token: {}", response.body());
                return null;
            }

            String token = node.get("access_token").asText();
            int expiresIn = node.has("expires_in") ? node.get("expires_in").asInt() : 300;

            // Cache the token with expiration time (minus buffer)
            cachedToken = token;
            tokenExpiration = Instant.now().plusSeconds(expiresIn - TOKEN_EXPIRY_BUFFER);

            log.info("AUTHENTICATION SUCCESS: Successfully obtained token from Availity");
            log.debug("Token: {}...{}", token.substring(0, 5), token.substring(token.length() - 5));
            log.debug("Expires in: {} seconds", expiresIn);
            log.debug("Cached until: {}", tokenExpiration);

            return token;

        } catch (Exception e) {
            log.error("AUTHENTICATION ERROR: Exception while fetching token", e);
            log.error("Error: {}", e.getMessage());
            log.error("URL: {}", tokenUrl);
            return null;
        }
    }
}
