package com.example.fhirpriorauth.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirClientConfig {

    @Value("${fhir.base-url}")
    private String fhirBaseUrl;

    private final AuthInterceptor authInterceptor;

    public FhirClientConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * Create a FHIR context for R4 that can be reused across the application
     */
    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    /**
     * Create a FHIR client using the context
     */
    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext) {
        // Log the FHIR base URL
        System.out.println("Configuring FHIR client with base URL: " + fhirBaseUrl);

        // Create a client using the context
        IGenericClient client = fhirContext.newRestfulGenericClient(fhirBaseUrl);

        // Register the auth interceptor
        client.registerInterceptor(authInterceptor);

        // Add a logging interceptor to log requests and responses
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestBody(true);
        loggingInterceptor.setLogResponseBody(true);
        loggingInterceptor.setLogRequestHeaders(true);
        loggingInterceptor.setLogResponseHeaders(true);
        client.registerInterceptor(loggingInterceptor);

        // Add a custom interceptor to add required headers for Availity
        client.registerInterceptor(new IClientInterceptor() {
            @Override
            public void interceptRequest(IHttpRequest request) {
                // Add any additional headers required by Availity
                request.addHeader("Accept", "application/json");
                request.addHeader("Content-Type", "application/json");

                // Add X-Response-Encoding-Context header for XSS prevention
                request.addHeader("X-Response-Encoding-Context", "HTML");
            }

            @Override
            public void interceptResponse(IHttpResponse response) {
                // No action needed for response
            }
        });

        return client;
    }
}
