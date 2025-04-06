package com.example.fhirpriorauth.controller;

import com.example.fhirpriorauth.service.AvailityPollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for polling prior authorization status
 */
@RestController
@RequestMapping("/prior-auth/polling")
public class PollingController {

    private static final Logger log = LoggerFactory.getLogger(PollingController.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AvailityPollingService availityPollingService;

    @Autowired
    public PollingController(AvailityPollingService availityPollingService) {
        this.availityPollingService = availityPollingService;
    }

    /**
     * Get the status of all prior auths being polled
     */
    @GetMapping
    public ResponseEntity<?> getAllPollingStatuses() {
        try {
            log.info("Getting all polling statuses");
            
            // Create a map to hold the statuses
            Map<String, Object> statuses = new HashMap<>();
            
            // In the current implementation, we don't have a way to get all prior auth statuses
            // So we'll just return an empty map
            
            return ResponseEntity.ok(statuses);
        } catch (Exception e) {
            log.error("Error getting all polling statuses", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error getting all polling statuses: " + e.getMessage()));
        }
    }
    
    /**
     * Get the status of a specific prior auth being polled
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPollingStatus(@PathVariable String id) {
        try {
            log.info("Getting polling status for prior auth ID: {}", id);
            
            // In the current implementation, we don't have a way to get the status of a specific prior auth
            // So we'll just return a placeholder status
            
            // Convert to a map of status information
            Map<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("id", id);
            statusInfo.put("status", "0");
            statusInfo.put("statusDescription", getStatusDescription("0"));
            statusInfo.put("lastPolled", LocalDateTime.now().format(DATE_TIME_FORMATTER));
            statusInfo.put("pollingAttempts", 1);
            
            return ResponseEntity.ok(statusInfo);
        } catch (Exception e) {
            log.error("Error getting polling status for prior auth ID: {}", id, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error getting polling status: " + e.getMessage()));
        }
    }
    
    /**
     * Start polling for a specific prior auth
     */
    @GetMapping("/start/{id}")
    public ResponseEntity<?> startPolling(@PathVariable String id) {
        try {
            log.info("Starting polling for prior auth ID: {}", id);
            
            // Start polling for the prior auth ID
            availityPollingService.pollStatus(id, "dummy-token");
            
            return ResponseEntity.ok(Map.of(
                    "message", "Started polling for prior auth ID: " + id,
                    "status", "Polling started"
            ));
        } catch (Exception e) {
            log.error("Error starting polling for prior auth ID: {}", id, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error starting polling: " + e.getMessage()));
        }
    }
    
    /**
     * Get a description for a status code
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
            case "202":
                return "Accepted (Processing)";
            case "TIMEOUT":
                return "Polling timeout";
            default:
                return "Unknown status: " + statusCode;
        }
    }
}
