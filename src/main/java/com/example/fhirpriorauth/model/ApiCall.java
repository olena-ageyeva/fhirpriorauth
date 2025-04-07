package com.example.fhirpriorauth.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Model class for tracking API calls
 */
public class ApiCall {
    private String id;
    private String requestId;
    private String endpoint;
    private String method;
    private String clientIp;
    private LocalDateTime timestamp;
    private String status;
    private Map<String, Object> fhirPayload;
    private Map<String, Object> availityPayload;
    private Map<String, Object> response;

    public ApiCall() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getFhirPayload() {
        return fhirPayload;
    }

    public void setFhirPayload(Map<String, Object> fhirPayload) {
        this.fhirPayload = fhirPayload;
    }

    public Map<String, Object> getAvailityPayload() {
        return availityPayload;
    }

    public void setAvailityPayload(Map<String, Object> availityPayload) {
        this.availityPayload = availityPayload;
    }

    public Map<String, Object> getResponse() {
        return response;
    }

    public void setResponse(Map<String, Object> response) {
        this.response = response;
    }
}
