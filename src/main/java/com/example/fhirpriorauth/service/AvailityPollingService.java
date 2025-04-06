package com.example.fhirpriorauth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AvailityPollingService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void pollStatus(String id, String accessToken) {
        String url = "https://api.availity.com/availity/v2/service-reviews/" + id;

        try {
            var response = restTemplate.getForEntity(
                url,
                String.class,
                accessToken
            );
            System.out.println("✅ Status response for ID " + id + ": " + response.getBody());
        } catch (Exception e) {
            System.err.println("❌ Polling failed for ID " + id + ": " + e.getMessage());
        }
    }
}
