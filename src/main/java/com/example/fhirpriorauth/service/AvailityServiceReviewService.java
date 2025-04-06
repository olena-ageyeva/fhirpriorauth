package com.example.fhirpriorauth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service for interacting with Availity's Service Reviews API
 */
@Service
public class AvailityServiceReviewService {

    private static final Logger log = LoggerFactory.getLogger(AvailityServiceReviewService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${availity.api.service-reviews-url}")
    private String serviceReviewsUrl;

    private final com.example.fhirpriorauth.auth.TokenService tokenService;

    // Store the last response JSON for display in the UI
    private String lastResponseJson = "{\"status\": \"No data available yet\"}";

    @Autowired
    public AvailityServiceReviewService(com.example.fhirpriorauth.auth.TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostConstruct
    public void init() {
        log.info("AvailityServiceReviewService initialized with URL: {}", serviceReviewsUrl);
    }

    /**
     * Submit a service review (prior authorization) request to Availity
     *
     * @return The ID of the service review request
     */
    public String submitServiceReview() {
        return submitServiceReview(true); // Use mock data by default
    }

    /**
     * Submit a service review (prior authorization) request to Availity
     *
     * @param useMockData Whether to use mock data from the mock-data file
     * @return The ID of the service review request
     */
    public String submitServiceReview(boolean useMockData) {
        try {
            log.info("Submitting service review to Availity at URL: {}", serviceReviewsUrl);

            // Get the access token
            String token = tokenService.fetchAccessToken();
            log.info("Successfully obtained access token for Availity API");

            // Create the request body
            String requestBody;
            if (useMockData) {
                // Read the mock data file
                try {
                    java.nio.file.Path path = java.nio.file.Paths.get("src/main/resources/mock-data/mock-service-review.json");
                    requestBody = new String(java.nio.file.Files.readAllBytes(path));
                    log.info("Using mock data from file: {}", path);
                } catch (Exception e) {
                    log.error("Failed to read mock data file, falling back to generated data", e);
                    requestBody = createServiceReviewRequest();
                }
            } else {
                requestBody = createServiceReviewRequest();
            }

            log.info("Created service review request body with {} characters", requestBody.length());
            log.debug("Service review request body: {}", requestBody);

            // Print the request details to the console
            System.out.println("⏩ SENDING PRIOR AUTH REQUEST TO AVAILITY");
            System.out.println("   URL: " + serviceReviewsUrl);
            System.out.println("   Method: POST");
            System.out.println("   Headers: Authorization, Content-Type, Accept" + (useMockData ? ", X-Api-Mock-Scenario-ID" : ""));

            // Create the HTTP request builder
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serviceReviewsUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            // Add mock scenario header if using mock data
            if (useMockData) {
                requestBuilder.header("X-Api-Mock-Scenario-ID", "SR-CreateRequestAccepted-i");
                System.out.println("   Using mock scenario: SR-CreateRequestAccepted-i");
            }

            // Build the request with the body
            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Send the request
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check the response
            int statusCode = response.statusCode();
            String responseBody = response.body();
            log.info("Received response from Availity with status code: {}", statusCode);
            log.debug("Service review response body: {}", responseBody);

            // Log all response headers
            log.debug("Response headers:");
            response.headers().map().forEach((key, values) -> {
                log.debug("   {}: {}", key, String.join(", ", values));
            });

            if (statusCode == 202) {
                // Success - get the location header
                String location = response.headers().firstValue("Location").orElse(null);
                if (location != null) {
                    // Extract the ID from the location
                    String id = location.substring(location.lastIndexOf("/") + 1);
                    log.info("Service review submitted successfully. ID: {}", id);

                    // Print success message
                    System.out.println("\n✅ PRIOR AUTH SUBMITTED: Successfully submitted service review to Availity");
                    System.out.println("   Status: 202 Accepted");
                    System.out.println("   Resource ID: " + id);
                    System.out.println("   Location: " + location);
                    System.out.println("   Poll URL: " + serviceReviewsUrl + "/" + id);
                    System.out.println("\nTo check status, use: curl http://localhost:8080/prior-auth/submit/" + id);

                    return id;
                } else {
                    log.error("Service review submitted but no location header found");
                    throw new RuntimeException("Service review submitted but no location header found");
                }
            } else {
                // Error
                log.error("Failed to submit service review. Status: {}, Response: {}", statusCode, responseBody);

                // Print error message
                System.out.println("\n❌ PRIOR AUTH ERROR: Failed to submit service review to Availity");
                System.out.println("   Status: " + statusCode);
                System.out.println("   URL: " + serviceReviewsUrl);
                System.out.println("   Response: " + responseBody);

                // Try to parse the response as JSON for more details
                try {
                    JsonNode errorNode = objectMapper.readTree(responseBody);
                    if (errorNode.has("validationMessages") && errorNode.get("validationMessages").isArray()) {
                        System.out.println("   Validation Messages:");
                        for (JsonNode message : errorNode.get("validationMessages")) {
                            System.out.println("      - " + message.asText());
                        }
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }

                throw new RuntimeException("Failed to submit service review. Status: " + statusCode);
            }
        } catch (Exception e) {
            log.error("Error submitting service review", e);
            throw new RuntimeException("Error submitting service review: " + e.getMessage(), e);
        }
    }

    /**
     * Check the status of a service review
     *
     * @param id The ID of the service review
     * @return The status of the service review
     */
    public String checkServiceReviewStatus(String id) {
        return checkServiceReviewStatus(id, false);
    }

    /**
     * Check the status of a service review with mock scenario
     *
     * @param id The ID of the service review
     * @return The status of the service review
     */
    public String checkServiceReviewStatusWithMock(String id) {
        return checkServiceReviewStatus(id, true);
    }

    /**
     * Check the status of a service review
     *
     * @param id The ID of the service review
     * @param useMockScenario Whether to use a mock scenario
     * @return The status of the service review
     */
    private String checkServiceReviewStatus(String id, boolean useMockScenario) {
        try {
            log.info("Checking status of service review: {}", id);

            // Get the access token
            String token = tokenService.fetchAccessToken();
            if (token == null) {
                throw new RuntimeException("Failed to obtain access token from Availity");
            }

            // Create the HTTP request builder
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serviceReviewsUrl + "/" + id))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json");

            // Add mock scenario header if using mock scenario
            if (useMockScenario) {
                requestBuilder.header("X-Api-Mock-Scenario-ID", "SR-GetComplete-i");
                System.out.println("   Using mock scenario: SR-GetComplete-i");
            }

            // Build the request
            HttpRequest request = requestBuilder.GET().build();

            // Send the request
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check the response
            int statusCode = response.statusCode();
            String responseBody = response.body();
            log.debug("Service review status response: {} - {}", statusCode, responseBody);

            if (statusCode == 200) {
                // Success - parse the response
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // Store the response JSON for display in the UI
                lastResponseJson = responseBody;

                // Extract status information
                String statusCodeValue = jsonNode.has("statusCode") ? jsonNode.get("statusCode").asText() : "unknown";
                String statusText = jsonNode.has("status") ? jsonNode.get("status").asText() : "Unknown";

                // Map Availity status codes to our internal status codes
                String mappedStatus = mapAvailityStatus(statusCodeValue, statusText);

                // Print status message
                System.out.println("ℹ️ SERVICE REVIEW STATUS: " + statusText + " (" + statusCodeValue + ")");
                System.out.println("   ID: " + id);
                System.out.println("   Status Code: " + statusCodeValue);
                System.out.println("   Status Text: " + statusText);
                System.out.println("   Mapped Status: " + mappedStatus);

                // Print the full response for debugging
                System.out.println("   Full Response: " + responseBody.substring(0, Math.min(200, responseBody.length())) + "...");

                // Check for status reasons
                if (jsonNode.has("statusReasons") && jsonNode.get("statusReasons").isArray() &&
                        jsonNode.get("statusReasons").size() > 0) {
                    System.out.println("   Status Reasons:");
                    for (JsonNode reason : jsonNode.get("statusReasons")) {
                        String reasonCode = reason.has("code") ? reason.get("code").asText() : "";
                        String reasonValue = reason.has("value") ? reason.get("value").asText() : "";
                        System.out.println("      - " + reasonValue + " (" + reasonCode + ")");
                    }
                }

                // Check for validation messages
                if (jsonNode.has("validationMessages") && jsonNode.get("validationMessages").isArray() &&
                        jsonNode.get("validationMessages").size() > 0) {
                    System.out.println("   Validation Messages:");
                    for (JsonNode message : jsonNode.get("validationMessages")) {
                        System.out.println("      - " + message.asText());
                    }
                }

                return mappedStatus;
            } else {
                // Error
                log.error("Failed to check service review status. Status: {}, Response: {}", statusCode, responseBody);

                // Print error message
                System.out.println("❌ SERVICE REVIEW STATUS ERROR: Failed to check service review status");
                System.out.println("   Status: " + statusCode);
                System.out.println("   Response: " + responseBody);

                throw new RuntimeException("Failed to check service review status. Status: " + statusCode);
            }
        } catch (Exception e) {
            log.error("Error checking service review status", e);
            throw new RuntimeException("Error checking service review status: " + e.getMessage(), e);
        }
    }

    /**
     * Poll for the status of a service review until it's complete
     *
     * @param id The ID of the service review
     * @param maxAttempts Maximum number of polling attempts
     * @param delayMillis Delay between polling attempts in milliseconds
     * @return The final status of the service review
     */
    public String pollServiceReviewStatus(String id, int maxAttempts, long delayMillis) {
        return pollServiceReviewStatus(id, maxAttempts, delayMillis, true); // Use mock scenario by default
    }

    /**
     * Poll for the status of a service review until it's complete
     *
     * @param id The ID of the service review
     * @param maxAttempts Maximum number of polling attempts
     * @param delayMillis Delay between polling attempts in milliseconds
     * @param useMockScenario Whether to use a mock scenario
     * @return The final status of the service review
     */
    private String pollServiceReviewStatus(String id, int maxAttempts, long delayMillis, boolean useMockScenario) {
        log.info("Polling for service review status: {}", id);

        String status = "0"; // Start with "In Process"
        int attempts = 0;

        while (attempts < maxAttempts && "0".equals(status)) {
            try {
                // Wait before checking again
                if (attempts > 0) {
                    Thread.sleep(delayMillis);
                }

                // Check the status
                status = useMockScenario ? checkServiceReviewStatusWithMock(id) : checkServiceReviewStatus(id);
                attempts++;

                // If still in process, log and continue
                if ("0".equals(status)) {
                    log.info("Service review still in process. Attempt: {}/{}", attempts, maxAttempts);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Polling interrupted", e);
                throw new RuntimeException("Polling interrupted", e);
            }
        }

        if ("0".equals(status) && attempts >= maxAttempts) {
            log.warn("Reached maximum polling attempts. Service review still in process.");
            return "TIMEOUT";
        }

        return status;
    }

    /**
     * Create a service review request body
     *
     * @return The request body as a JSON string
     */
    private String createServiceReviewRequest() {
        try {
            ObjectNode requestNode = objectMapper.createObjectNode();

            // Payer information
            ObjectNode payerNode = requestNode.putObject("payer");
            payerNode.put("id", "BCBSF");
            payerNode.put("name", "FLORIDA BLUE");

            // Requesting provider information
            ObjectNode providerNode = requestNode.putObject("requestingProvider");
            providerNode.put("lastName", "Doe");
            providerNode.put("npi", "1234567890");
            providerNode.put("submitterId", "G12345");
            providerNode.put("specialtyCode", "207T00000X");
            providerNode.put("addressLine1", "321 Main St");
            providerNode.put("city", "JACKSONVILLE");
            providerNode.put("stateCode", "FL");
            providerNode.put("zipCode", "32223");
            providerNode.put("contactName", "John Doe");
            providerNode.put("phone", "9043334444");

            // Subscriber information
            ObjectNode subscriberNode = requestNode.putObject("subscriber");
            subscriberNode.put("firstName", "Jane");
            subscriberNode.put("middleName", "J");
            subscriberNode.put("lastName", "Smith");
            subscriberNode.put("suffix", "JR");
            subscriberNode.put("memberId", "TEST1");
            subscriberNode.put("addressLine1", "123 MAIN ST");
            subscriberNode.put("addressLine2", "APT 3");
            subscriberNode.put("city", "JACKSONVILLE");
            subscriberNode.put("stateCode", "FL");
            subscriberNode.put("zipCode", "12345");

            // Patient information
            ObjectNode patientNode = requestNode.putObject("patient");
            patientNode.put("firstName", "Jane");
            patientNode.put("middleName", "J");
            patientNode.put("lastName", "Smith");
            patientNode.put("suffix", "JR");
            patientNode.put("subscriberRelationshipCode", "18");
            patientNode.put("birthDate", "1990-01-01");
            patientNode.put("genderCode", "F");

            // Request information
            requestNode.put("requestTypeCode", "HS"); // HS = Outpatient

            // Set dates (current date to 5 days later)
            LocalDate fromDate = LocalDate.now();
            LocalDate toDate = fromDate.plusDays(5);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            requestNode.put("fromDate", fromDate.format(formatter));
            requestNode.put("toDate", toDate.format(formatter));

            return objectMapper.writeValueAsString(requestNode);
        } catch (Exception e) {
            log.error("Error creating service review request", e);
            throw new RuntimeException("Error creating service review request: " + e.getMessage(), e);
        }
    }

    /**
     * Map Availity status codes to our internal status codes
     *
     * @param statusCode The Availity status code
     * @param statusText The Availity status text
     * @return Our internal status code
     */
    private String mapAvailityStatus(String statusCode, String statusText) {
        // Map Availity status codes to our internal status codes
        if (statusCode.equals("A4") || statusText.contains("Pended")) {
            return "4"; // Complete
        } else if (statusCode.equals("0") || statusText.contains("In Process")) {
            return "0"; // In Process
        } else if (statusCode.equals("400") || statusText.contains("Error")) {
            return "400"; // Error
        } else if (statusCode.equals("504") || statusText.contains("Timeout")) {
            return "504"; // Timeout
        } else {
            return statusCode; // Unknown
        }
    }

    /**
     * Get the last response JSON
     *
     * @return The last response JSON
     */
    public String getLastResponseJson() {
        return lastResponseJson;
    }

    /**
     * Get a description for a status code
     *
     * @param statusCode The status code
     * @return A description of the status
     */
    private String getStatusDescription(String statusCode) {
        switch (statusCode) {
            case "0":
                return "In Process";
            case "4":
                return "Complete";
            case "A4":
                return "Pended (Complete)";
            case "400":
                return "Error";
            case "504":
                return "Timeout from health plan";
            case "TIMEOUT":
                return "Polling timeout";
            default:
                return "Unknown status: " + statusCode;
        }
    }
}
