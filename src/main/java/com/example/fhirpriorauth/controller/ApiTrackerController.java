package com.example.fhirpriorauth.controller;

import com.example.fhirpriorauth.model.ApiCall;
import com.example.fhirpriorauth.service.ApiTrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the API tracker
 */
@RestController
@RequestMapping("/api/tracker")
public class ApiTrackerController {
    private static final Logger log = LoggerFactory.getLogger(ApiTrackerController.class);
    
    private final ApiTrackerService apiTrackerService;
    
    @Autowired
    public ApiTrackerController(ApiTrackerService apiTrackerService) {
        this.apiTrackerService = apiTrackerService;
    }
    
    /**
     * Get API calls with filtering and pagination
     * 
     * @param endpoint The endpoint to filter by
     * @param dateRange The date range to filter by
     * @param page The page number (1-based)
     * @param pageSize The page size
     * @return A map containing the filtered API calls and pagination info
     */
    @GetMapping("/calls")
    public ResponseEntity<?> getApiCalls(
            @RequestParam(required = false, defaultValue = "all") String endpoint,
            @RequestParam(required = false, defaultValue = "all") String dateRange,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        
        try {
            Map<String, Object> result = apiTrackerService.getApiCalls(endpoint, dateRange, page, pageSize);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting API calls", e);
            return ResponseEntity.badRequest().body("Error getting API calls: " + e.getMessage());
        }
    }
    
    /**
     * Get a specific API call by ID
     * 
     * @param id The API call ID
     * @return The API call details
     */
    @GetMapping("/calls/{id}")
    public ResponseEntity<?> getApiCall(@PathVariable String id) {
        try {
            ApiCall apiCall = apiTrackerService.getApiCall(id);
            
            if (apiCall == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(apiCall);
        } catch (Exception e) {
            log.error("Error getting API call", e);
            return ResponseEntity.badRequest().body("Error getting API call: " + e.getMessage());
        }
    }
    
    /**
     * Clear all API call logs
     * 
     * @return A success message
     */
    @DeleteMapping("/calls")
    public ResponseEntity<?> clearApiCalls() {
        try {
            apiTrackerService.clearApiCalls();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "API call logs cleared successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing API calls", e);
            return ResponseEntity.badRequest().body("Error clearing API calls: " + e.getMessage());
        }
    }
}
