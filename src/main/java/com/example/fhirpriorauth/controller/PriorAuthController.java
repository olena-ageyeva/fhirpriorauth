package com.example.fhirpriorauth.controller;

import com.example.fhirpriorauth.service.PriorAuthService;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prior-auth")
public class PriorAuthController {

    private final PriorAuthService priorAuthService;

    @Autowired
    public PriorAuthController(PriorAuthService priorAuthService) {
        this.priorAuthService = priorAuthService;
    }

    // Removed /submit endpoint - now handled by SubmitController

    @PostMapping
    public ResponseEntity<String> submitPriorAuth(@RequestBody(required = false) Claim claim) {
        if (claim == null) {
            claim = new Claim(); // Create a default claim if none provided
        }

        try {
            ClaimResponse response = priorAuthService.submitPriorAuth(claim);
            String resourceId = response.getId();

            if (resourceId != null && resourceId.startsWith("error-")) {
                return ResponseEntity.status(500).body("Error - Failed to submit prior auth to Availity");
            }

            return ResponseEntity.ok("Success. Prior Auth ID: " + resourceId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error - " + e.getMessage());
        }
    }

    @GetMapping("/{claimId}")
    public ResponseEntity<String> checkStatus(@PathVariable String claimId) {
        try {
            ClaimResponse response = priorAuthService.checkPriorAuthStatus(claimId);
            if (response == null) {
                return ResponseEntity.notFound().build();
            }

            String status = response.getOutcome() != null ? response.getOutcome().getDisplay() : "PENDING";
            return ResponseEntity.ok("Prior Auth Status: " + status + ", ID: " + claimId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error - " + e.getMessage());
        }
    }

    @DeleteMapping("/{claimId}")
    public ResponseEntity<String> cancelPriorAuth(@PathVariable String claimId) {
        try {
            boolean success = priorAuthService.cancelPriorAuth(claimId);
            if (success) {
                return ResponseEntity.ok("Success. Prior auth cancelled: " + claimId);
            } else {
                return ResponseEntity.status(500).body("Error - Failed to cancel prior auth");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error - " + e.getMessage());
        }
    }

    @GetMapping
    public String welcome() {
        return "Use POST to submit a FHIR Prior Authorization request.";
    }

    @RequestMapping(value = "/test", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> testConnection() {
        try {
            String id = priorAuthService.sendPriorAuthRequest();
            if (id != null && id.startsWith("error-")) {
                return ResponseEntity.status(500).body("Error - Failed to connect to Availity. Authentication failed or server unavailable. Check credentials and network.");
            }
            return ResponseEntity.ok("Success. Connected to Availity. Test ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error - " + e.getMessage());
        }
    }


}
