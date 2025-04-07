package com.example.fhirpriorauth.service;

import com.example.fhirpriorauth.model.ApiCall;
import com.example.fhirpriorauth.util.FhirToAvailityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for tracking API calls
 */
@Service
public class ApiTrackerService {
    private static final Logger log = LoggerFactory.getLogger(ApiTrackerService.class);
    
    private final Map<String, ApiCall> apiCalls = new ConcurrentHashMap<>();
    private final FhirToAvailityMapper mapper;
    
    @Autowired
    public ApiTrackerService(FhirToAvailityMapper mapper) {
        this.mapper = mapper;
    }
    
    /**
     * Track a new API call
     * 
     * @param endpoint The API endpoint
     * @param method The HTTP method
     * @param clientIp The client IP address
     * @param payload The request payload (for POST requests)
     * @return The created ApiCall object
     */
    public ApiCall trackApiCall(String endpoint, String method, String clientIp, Map<String, Object> payload) {
        ApiCall apiCall = new ApiCall();
        apiCall.setEndpoint(endpoint);
        apiCall.setMethod(method);
        apiCall.setClientIp(clientIp);
        apiCall.setRequestId(generateRequestId());
        apiCall.setStatus("Pending");
        
        if (payload != null && endpoint.equals("/submit")) {
            apiCall.setFhirPayload(payload);
            
            try {
                // Convert FHIR to Availity format
                Map<String, Object> availityPayload = convertFhirToAvaility(payload);
                apiCall.setAvailityPayload(availityPayload);
            } catch (Exception e) {
                log.error("Error converting FHIR to Availity", e);
            }
        }
        
        apiCalls.put(apiCall.getId(), apiCall);
        log.info("Tracked API call: {} {} from {}", method, endpoint, clientIp);
        
        return apiCall;
    }
    
    /**
     * Update the status and response of an API call
     * 
     * @param requestId The request ID
     * @param status The new status
     * @param response The response data
     * @return The updated ApiCall object, or null if not found
     */
    public ApiCall updateApiCall(String requestId, String status, Map<String, Object> response) {
        Optional<ApiCall> apiCallOpt = apiCalls.values().stream()
                .filter(call -> call.getRequestId().equals(requestId))
                .findFirst();
        
        if (apiCallOpt.isPresent()) {
            ApiCall apiCall = apiCallOpt.get();
            apiCall.setStatus(status);
            apiCall.setResponse(response);
            
            log.info("Updated API call status: {} -> {}", requestId, status);
            return apiCall;
        }
        
        return null;
    }
    
    /**
     * Get an API call by ID
     * 
     * @param id The API call ID
     * @return The ApiCall object, or null if not found
     */
    public ApiCall getApiCall(String id) {
        return apiCalls.get(id);
    }
    
    /**
     * Get API calls filtered by endpoint and date range
     * 
     * @param endpoint The API endpoint to filter by
     * @param dateRange The date range to filter by
     * @param page The page number (1-based)
     * @param pageSize The page size
     * @return A map containing the filtered API calls and pagination info
     */
    public Map<String, Object> getApiCalls(String endpoint, String dateRange, int page, int pageSize) {
        List<ApiCall> filteredCalls = apiCalls.values().stream()
                .filter(call -> filterByEndpoint(call, endpoint))
                .filter(call -> filterByDateRange(call, dateRange))
                .sorted(Comparator.comparing(ApiCall::getTimestamp).reversed())
                .collect(Collectors.toList());
        
        // Calculate pagination
        int totalCalls = filteredCalls.size();
        int totalPages = (int) Math.ceil((double) totalCalls / pageSize);
        
        // Apply pagination
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalCalls);
        
        List<ApiCall> pagedCalls = fromIndex < totalCalls 
                ? filteredCalls.subList(fromIndex, toIndex) 
                : Collections.emptyList();
        
        // Create result map
        Map<String, Object> result = new HashMap<>();
        result.put("calls", pagedCalls);
        result.put("totalCalls", totalCalls);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        
        return result;
    }
    
    /**
     * Clear all API call logs
     */
    public void clearApiCalls() {
        apiCalls.clear();
        log.info("Cleared all API call logs");
    }
    
    /**
     * Generate a unique request ID
     * 
     * @return A unique request ID
     */
    private String generateRequestId() {
        return "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Filter API calls by endpoint
     * 
     * @param apiCall The API call to filter
     * @param endpoint The endpoint to filter by
     * @return true if the API call matches the endpoint filter
     */
    private boolean filterByEndpoint(ApiCall apiCall, String endpoint) {
        return endpoint == null || endpoint.equals("all") || apiCall.getEndpoint().equals(endpoint);
    }
    
    /**
     * Filter API calls by date range
     * 
     * @param apiCall The API call to filter
     * @param dateRange The date range to filter by
     * @return true if the API call matches the date range filter
     */
    private boolean filterByDateRange(ApiCall apiCall, String dateRange) {
        if (dateRange == null || dateRange.equals("all")) {
            return true;
        }
        
        LocalDateTime timestamp = apiCall.getTimestamp();
        LocalDate today = LocalDate.now();
        
        switch (dateRange) {
            case "today":
                return timestamp.toLocalDate().equals(today);
            case "yesterday":
                return timestamp.toLocalDate().equals(today.minusDays(1));
            case "week":
                return timestamp.isAfter(LocalDateTime.of(today.minusDays(7), LocalTime.MIDNIGHT));
            case "month":
                return timestamp.isAfter(LocalDateTime.of(today.minusDays(30), LocalTime.MIDNIGHT));
            default:
                return true;
        }
    }
    
    /**
     * Convert FHIR payload to Availity format
     * 
     * @param fhirPayload The FHIR payload
     * @return The Availity payload
     */
    private Map<String, Object> convertFhirToAvaility(Map<String, Object> fhirPayload) {
        // Create a simple Availity service review object based on the FHIR data
        Map<String, Object> serviceReview = new HashMap<>();
        
        // Payer information
        Map<String, Object> payer = new HashMap<>();
        payer.put("id", "BCBSF");
        payer.put("name", "FLORIDA BLUE");
        serviceReview.put("payer", payer);
        
        // Provider information
        Map<String, Object> provider = new HashMap<>();
        provider.put("npi", "1234567893");
        if (fhirPayload.containsKey("provider") && ((Map<String, Object>)fhirPayload.get("provider")).containsKey("display")) {
            provider.put("lastName", ((Map<String, Object>)fhirPayload.get("provider")).get("display"));
        } else {
            provider.put("lastName", "PROVIDER");
        }
        provider.put("firstName", "TEST");
        provider.put("roleCode", "1P");
        provider.put("addressLine1", "123 Provider Street");
        provider.put("city", "JACKSONVILLE");
        provider.put("stateCode", "FL");
        provider.put("zipCode", "32223");
        provider.put("phone", "9043334444");
        provider.put("contactName", "John Doe");
        serviceReview.put("requestingProvider", provider);
        
        // Subscriber information
        Map<String, Object> subscriber = new HashMap<>();
        subscriber.put("memberId", "ASBA1274712");
        
        // Extract patient name from FHIR
        if (fhirPayload.containsKey("patient") && ((Map<String, Object>)fhirPayload.get("patient")).containsKey("display")) {
            String patientDisplay = (String)((Map<String, Object>)fhirPayload.get("patient")).get("display");
            String[] parts = patientDisplay.split(" ", 2);
            if (parts.length > 1) {
                subscriber.put("firstName", parts[0]);
                subscriber.put("lastName", parts[1]);
            } else {
                subscriber.put("firstName", "TEST");
                subscriber.put("lastName", patientDisplay);
            }
        } else {
            subscriber.put("firstName", "TEST");
            subscriber.put("lastName", "PATIENT");
        }
        serviceReview.put("subscriber", subscriber);
        
        // Patient information
        Map<String, Object> patient = new HashMap<>();
        patient.put("firstName", "TEST");
        patient.put("lastName", "PATIENTONE");
        patient.put("subscriberRelationshipCode", "18");
        patient.put("birthDate", "1990-01-01");
        serviceReview.put("patient", patient);
        
        // Diagnoses
        List<Map<String, Object>> diagnoses = new ArrayList<>();
        if (fhirPayload.containsKey("diagnosis")) {
            List<Map<String, Object>> diagList = (List<Map<String, Object>>) fhirPayload.get("diagnosis");
            for (Map<String, Object> diag : diagList) {
                Map<String, Object> diagnosis = new HashMap<>();
                diagnosis.put("qualifierCode", "ABK");
                
                // Try to extract code from FHIR diagnosis
                String code = "78900";
                if (diag.containsKey("diagnosisCodeableConcept") && 
                    ((Map<String, Object>)diag.get("diagnosisCodeableConcept")).containsKey("coding")) {
                    List<Map<String, Object>> codings = 
                        (List<Map<String, Object>>)((Map<String, Object>)diag.get("diagnosisCodeableConcept")).get("coding");
                    if (!codings.isEmpty() && codings.get(0).containsKey("code")) {
                        code = (String)codings.get(0).get("code");
                    }
                }
                diagnosis.put("code", code);
                diagnoses.add(diagnosis);
            }
        }
        if (diagnoses.isEmpty()) {
            Map<String, Object> diagnosis = new HashMap<>();
            diagnosis.put("qualifierCode", "ABK");
            diagnosis.put("code", "78900");
            diagnoses.add(diagnosis);
        }
        serviceReview.put("diagnoses", diagnoses);
        
        // Request metadata
        serviceReview.put("requestTypeCode", "HS");
        serviceReview.put("serviceTypeCode", "73");
        serviceReview.put("placeOfServiceCode", "22");
        serviceReview.put("serviceLevelCode", "E");
        serviceReview.put("fromDate", "2022-09-02");
        serviceReview.put("toDate", "2022-09-13");
        serviceReview.put("quantity", "1");
        serviceReview.put("quantityTypeCode", "VS");
        
        // Procedures
        List<Map<String, Object>> procedures = new ArrayList<>();
        if (fhirPayload.containsKey("procedure")) {
            List<Map<String, Object>> procList = (List<Map<String, Object>>) fhirPayload.get("procedure");
            for (Map<String, Object> proc : procList) {
                Map<String, Object> procedure = new HashMap<>();
                procedure.put("fromDate", proc.containsKey("date") ? proc.get("date") : "2022-09-02");
                procedure.put("toDate", "2022-09-13");
                
                // Try to extract code from FHIR procedure
                String code = "99213";
                if (proc.containsKey("procedureCodeableConcept") && 
                    ((Map<String, Object>)proc.get("procedureCodeableConcept")).containsKey("coding")) {
                    List<Map<String, Object>> codings = 
                        (List<Map<String, Object>>)((Map<String, Object>)proc.get("procedureCodeableConcept")).get("coding");
                    if (!codings.isEmpty() && codings.get(0).containsKey("code")) {
                        code = (String)codings.get(0).get("code");
                    }
                }
                procedure.put("code", code);
                procedure.put("qualifierCode", "HC");
                procedure.put("quantity", "1");
                procedure.put("quantityTypeCode", "UN");
                procedures.add(procedure);
            }
        }
        if (procedures.isEmpty()) {
            Map<String, Object> procedure = new HashMap<>();
            procedure.put("fromDate", "2022-09-02");
            procedure.put("toDate", "2022-09-13");
            procedure.put("code", "99213");
            procedure.put("qualifierCode", "HC");
            procedure.put("quantity", "1");
            procedure.put("quantityTypeCode", "UN");
            procedures.add(procedure);
        }
        serviceReview.put("procedures", procedures);
        
        // Rendering Providers
        List<Map<String, Object>> renderingProviders = new ArrayList<>();
        Map<String, Object> renderingProvider = new HashMap<>();
        renderingProvider.put("lastName", "PROVIDERONE");
        renderingProvider.put("firstName", "TEST");
        renderingProvider.put("npi", "1234567891");
        renderingProvider.put("taxId", "111111111");
        renderingProvider.put("roleCode", "71");
        renderingProvider.put("addressLine1", "111 HEALTHY PKWY");
        renderingProvider.put("city", "JACKSONVILLE");
        renderingProvider.put("stateCode", "FL");
        renderingProvider.put("zipCode", "22222");
        renderingProviders.add(renderingProvider);
        serviceReview.put("renderingProviders", renderingProviders);
        
        // Wrap in serviceReview object
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("serviceReview", serviceReview);
        
        return wrapper;
    }
}
