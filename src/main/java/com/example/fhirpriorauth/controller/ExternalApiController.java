package com.example.fhirpriorauth.controller;

import com.example.fhirpriorauth.model.ApiCall;
import com.example.fhirpriorauth.service.ApiTrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for handling external API calls
 */
@RestController
public class ExternalApiController {
    private static final Logger log = LoggerFactory.getLogger(ExternalApiController.class);

    private final ApiTrackerService apiTrackerService;

    @Autowired
    public ExternalApiController(ApiTrackerService apiTrackerService) {
        this.apiTrackerService = apiTrackerService;
    }

    /**
     * Handle external submit requests
     *
     * @param payload The request payload
     * @param request The HTTP request
     * @return A response with the request ID
     */
    @PostMapping("/submit")
    public ResponseEntity<?> handleSubmit(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        log.info("Received submit request from {}", clientIp);

        // Track the API call
        ApiCall apiCall = apiTrackerService.trackApiCall("/submit", "POST", clientIp, payload);

        // Process the request (in a real scenario, this would call the actual service)
        String requestId = apiCall.getRequestId();
        String resourceId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create response
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("resourceId", resourceId);
        response.put("status", "Success");
        response.put("message", "Prior authorization request submitted successfully");

        // Update the API call with the response
        apiTrackerService.updateApiCall(requestId, "Success", response);

        return ResponseEntity.ok(response);
    }

    /**
     * Handle external status requests
     *
     * @param id The resource ID
     * @param request The HTTP request
     * @return A response with the status
     */
    @GetMapping("/status")
    public ResponseEntity<?> handleStatus(@RequestParam String id, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        log.info("Received status request from {} for resource {}", clientIp, id);

        // Track the API call
        ApiCall apiCall = apiTrackerService.trackApiCall("/status", "GET", clientIp, null);

        // Process the request (in a real scenario, this would call the actual service)
        String requestId = apiCall.getRequestId();

        // Create response
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("resourceId", id);

        // Simulate different statuses based on the resource ID
        String status;
        if (id.endsWith("A")) {
            status = "Approved";
        } else if (id.endsWith("D")) {
            status = "Denied";
        } else if (id.endsWith("P")) {
            status = "Pending";
        } else {
            status = "In Review";
        }

        response.put("status", status);
        response.put("lastUpdated", java.time.LocalDateTime.now().toString());

        // Update the API call with the response
        apiTrackerService.updateApiCall(requestId, status, response);

        return ResponseEntity.ok(response);
    }

    /**
     * Get the client IP address from the request
     *
     * @param request The HTTP request
     * @return The client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
