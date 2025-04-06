package com.example.fhirpriorauth.config;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.example.fhirpriorauth.auth.TokenService;
import org.springframework.stereotype.Component;

@Component
public class AuthInterceptor implements IClientInterceptor {

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void interceptRequest(IHttpRequest request) {
        String token = tokenService.fetchAccessToken();
        request.addHeader("Authorization", "Bearer " + token);
    }

    @Override
    public void interceptResponse(IHttpResponse response) {
        // Optional: handle or log the response if needed
    }
} 
