package com.example.fhirpriorauth.controller;

import com.example.fhirpriorauth.service.AvailityServiceReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for submitting prior authorization requests
 */
@RestController
@RequestMapping("/prior-auth/submit")
public class SubmitController {

    private static final Logger log = LoggerFactory.getLogger(SubmitController.class);

    private final AvailityServiceReviewService availityServiceReviewService;

    @Autowired
    public SubmitController(AvailityServiceReviewService availityServiceReviewService) {
        this.availityServiceReviewService = availityServiceReviewService;
    }

    /**
     * Submit a test prior authorization request
     */
    @GetMapping
    public ResponseEntity<String> submitTestPriorAuth() {
        try {
            // Submit the service review using mock data
            log.info("Submitting test prior authorization request to Availity using mock data");
            String id = availityServiceReviewService.submitServiceReview(true); // Use mock data

            return ResponseEntity.ok("Submission Successful\n" +
                    "Successfully submitted prior authorization request to Availity.\n\n" +
                    "Resource ID: " + id);
        } catch (Exception e) {
            log.error("Error submitting test prior authorization request", e);
            return ResponseEntity.status(500)
                    .body("Error - " + e.getMessage());
        }
    }

    /**
     * Submit a test prior authorization request (POST method)
     */
    @PostMapping
    public ResponseEntity<String> submitTestPriorAuthPost() {
        try {
            // Submit the service review using mock data
            log.info("Submitting test prior authorization request to Availity using mock data (POST)");
            String id = availityServiceReviewService.submitServiceReview(true); // Use mock data

            return ResponseEntity.ok("Submission Successful\n" +
                    "Successfully submitted prior authorization request to Availity.\n\n" +
                    "Resource ID: " + id);
        } catch (Exception e) {
            log.error("Error submitting test prior authorization request", e);
            return ResponseEntity.status(500)
                    .body("Error - " + e.getMessage());
        }
    }

    /**
     * Check the status of a prior authorization request
     */
    @GetMapping("/{id}")
    public ResponseEntity<String> checkStatus(@PathVariable String id) {
        try {
            // Check the status
            log.info("Checking status of prior authorization request: {}", id);
            String status = availityServiceReviewService.checkServiceReviewStatus(id);

            return ResponseEntity.ok("Status Check\n" +
                    "Prior authorization status: " + getStatusDescription(status) + "\n" +
                    "Resource ID: " + id);
        } catch (Exception e) {
            log.error("Error checking status of prior authorization request", e);
            return ResponseEntity.status(500)
                    .body("Error - " + e.getMessage());
        }
    }

    /**
     * Poll for the status of a prior authorization request
     */
    @GetMapping("/{id}/poll")
    public ResponseEntity<String> pollStatus(@PathVariable String id) {
        try {
            // Poll for the status
            log.info("Polling for status of prior authorization request: {}", id);
            String status = availityServiceReviewService.pollServiceReviewStatus(id, 10, 2000);

            return ResponseEntity.ok("Prior authorization final status: " + getStatusDescription(status) +
                    "\nResource ID: " + id);
        } catch (Exception e) {
            log.error("Error polling for status of prior authorization request", e);
            return ResponseEntity.status(500)
                    .body("Error - " + e.getMessage());
        }
    }

    /**
     * Check the status of a prior authorization request with mock scenario
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<String> checkStatusWithMock(@PathVariable String id) {
        try {
            // Check the status with mock scenario
            log.info("Checking status of prior authorization request with mock scenario: {}", id);
            String status = availityServiceReviewService.checkServiceReviewStatusWithMock(id);
            String jsonResponse = availityServiceReviewService.getLastResponseJson();

            // Return a response with both the status and the JSON response
            return ResponseEntity.ok("Status: " + getStatusDescription(status) +
                    "\nResource ID: " + id +
                    "\n\n" + jsonResponse);
        } catch (Exception e) {
            log.error("Error checking status of prior authorization request with mock scenario", e);
            return ResponseEntity.status(500)
                    .body("Error - " + e.getMessage());
        }
    }

    /**
     * Get a description for a status code
     */
    public static String getStatusDescription(String statusCode) {
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
