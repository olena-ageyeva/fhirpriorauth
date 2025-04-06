package com.example.fhirpriorauth.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class PriorAuthService {

    private static final Logger log = LoggerFactory.getLogger(PriorAuthService.class);

    private final IGenericClient fhirClient;
    private final FhirContext fhirContext;

    @Autowired
    public PriorAuthService(IGenericClient fhirClient, FhirContext fhirContext) {
        this.fhirClient = fhirClient;
        this.fhirContext = fhirContext;
    }

    /**
     * Submits a prior authorization request to the FHIR server.
     *
     * @param claim The FHIR Claim resource representing the prior authorization request
     * @return The ClaimResponse from the FHIR server
     */
    public ClaimResponse submitPriorAuth(Claim claim) {
        log.info("Submitting prior authorization request to Availity");

        // Create a unique ID for this request
        String requestId = "priorauth-" + UUID.randomUUID().toString();

        // Prepare the claim with all required fields
        prepareClaim(claim, requestId);

        // Log the request details
        log.info("Sending to Availity FHIR server: {}" , fhirClient.getServerBase());
        String claimJson = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(claim);
        log.debug("Claim payload: {}", claimJson);

        // Print the request details to the console
        System.out.println("→ PRIOR AUTH REQUEST: Sending prior authorization request to Availity");
        System.out.println("   Server: " + fhirClient.getServerBase());
        System.out.println("   Claim ID: " + claim.getId());
        System.out.println("   Patient: " + claim.getPatient().getDisplay());
        System.out.println("   Provider: " + claim.getProvider().getDisplay());
        System.out.println("   Insurer: " + claim.getInsurer().getDisplay());

        // For debugging, print a snippet of the JSON payload
        System.out.println("   Payload snippet: " + claimJson.substring(0, Math.min(100, claimJson.length())) + "...");

        try {
            // Submit the claim to the FHIR server
            MethodOutcome outcome = fhirClient.create()
                .resource(claim)
                .prettyPrint()
                .encodedJson()
                .execute();

            String id = outcome.getId() != null ? outcome.getId().getValue() : requestId;
            log.info("Created resource ID: {}", id);

            // Print a clear success message
            System.out.println("✅ PRIOR AUTH SUBMITTED: Successfully submitted prior authorization to Availity");
            System.out.println("   Resource ID: " + id);
            System.out.println("   Server: " + fhirClient.getServerBase());

            // If we got a response body, print it
            if (outcome.getResource() != null) {
                String responseJson = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(outcome.getResource());
                System.out.println("   Response snippet: " + responseJson.substring(0, Math.min(100, responseJson.length())) + "...");
            }

            // Process the response
            return processClaimResponse(outcome, id);
        } catch (BaseServerResponseException e) {
            log.error("FHIR server error: {} - {}", e.getStatusCode(), e.getMessage());

            // Print a clear error message
            System.out.println("❌ PRIOR AUTH ERROR: Server returned an error");
            System.out.println("   Status code: " + e.getStatusCode());
            System.out.println("   Error: " + e.getMessage());
            System.out.println("   Server: " + fhirClient.getServerBase());

            // If there's an operation outcome, print it
            if (e.getOperationOutcome() != null) {
                String outcomeJson = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(e.getOperationOutcome());
                System.out.println("   Operation outcome: " + outcomeJson);
            }

            return createErrorResponse("Server error: " + e.getStatusCode() + " - " + e.getMessage());
        } catch (Exception e) {
            String rootCause = getRootCauseMessage(e);
            log.error("Error submitting prior auth to Availity: {}", rootCause, e);

            // Print a clear error message
            System.out.println("❌ PRIOR AUTH ERROR: Exception while submitting prior authorization");
            System.out.println("   Error: " + rootCause);
            System.out.println("   Server: " + fhirClient.getServerBase());

            return createErrorResponse("Connection error: " + rootCause);
        }
    }

    /**
     * Prepares a claim with all required fields for a valid FHIR prior authorization request
     */
    private void prepareClaim(Claim claim, String requestId) {
        // Set a request ID if not already set
        if (claim.getId() == null || claim.getId().isEmpty()) {
            claim.setId(requestId);
        }

        // Ensure claim has required fields
        if (claim.getCreated() == null) {
            claim.setCreated(new Date());
        }

        // Set claim type to prior-authorization if not already set
        if (claim.getUse() == null) {
            claim.setUse(Claim.Use.PREAUTHORIZATION);
        }

        if (claim.getStatus() == null) {
            claim.setStatus(Claim.ClaimStatus.ACTIVE);
        }

        // Add minimal required fields for a valid FHIR Claim
        if (claim.getProvider() == null) {
            claim.setProvider(new Reference().setDisplay("Example Provider"));
        }

        if (claim.getInsurer() == null) {
            claim.setInsurer(new Reference().setDisplay("Example Insurer"));
        }

        if (claim.getPatient() == null) {
            claim.setPatient(new Reference().setDisplay("Example Patient"));
        }

        // Add a type if not present (required by FHIR spec)
        if (claim.getType() == null) {
            CodeableConcept type = new CodeableConcept();
            type.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/claim-type")
                .setCode("professional")
                .setDisplay("Professional");
            claim.setType(type);
        }
    }

    /**
     * Process the claim response from the server
     */
    private ClaimResponse processClaimResponse(MethodOutcome outcome, String id) {
        // If we got a resource back from the server, use it
        if (outcome.getResource() instanceof ClaimResponse) {
            ClaimResponse response = (ClaimResponse) outcome.getResource();
            log.info("Received ClaimResponse from server");
            return response;
        }

        // Otherwise create a response based on the outcome
        ClaimResponse response = new ClaimResponse();
        response.setId(id);
        response.setStatus(ClaimResponse.ClaimResponseStatus.ACTIVE);
        response.setCreated(new Date());

        // Add a reference to the original claim
        response.setRequest(new Reference("Claim/" + id));

        return response;
    }

    /**
     * Create an error response when something goes wrong
     */
    private ClaimResponse createErrorResponse(String errorMessage) {
        ClaimResponse errorResponse = new ClaimResponse();
        errorResponse.setId("error-" + System.currentTimeMillis());
        errorResponse.setStatus(ClaimResponse.ClaimResponseStatus.ACTIVE);
        errorResponse.setCreated(new Date());

        // Add error information to the response
        errorResponse.setOutcome(ClaimResponse.RemittanceOutcome.ERROR);

        // Add error information using the available methods
        errorResponse.addError().setCode(
            new CodeableConcept().setText("Error: " + errorMessage)
        );

        return errorResponse;
    }

    /**
     * Checks the status of a prior authorization request.
     *
     * @param claimId The ID of the prior authorization claim
     * @return The ClaimResponse containing the status
     */
    public ClaimResponse checkPriorAuthStatus(String claimId) {
        log.info("Checking status for prior authorization ID: {}", claimId);

        try {
            // First try to get the claim itself to verify it exists
            Claim claim = fhirClient.read()
                    .resource(Claim.class)
                    .withId(claimId)
                    .execute();

            log.info("Found claim with ID: {}, status: {}", claimId, claim.getStatus());

            // Search for ClaimResponse resources related to the claim
            Bundle bundle = fhirClient.search()
                    .forResource(ClaimResponse.class)
                    .where(ClaimResponse.REQUEST.hasId(claimId))
                    .returnBundle(Bundle.class)
                    .execute();

            if (bundle.getEntry().isEmpty()) {
                log.warn("No response found for claim ID: {}", claimId);

                // Create a pending response if none exists yet
                ClaimResponse pendingResponse = new ClaimResponse();
                pendingResponse.setId("pending-" + claimId);
                pendingResponse.setStatus(ClaimResponse.ClaimResponseStatus.ACTIVE);
                pendingResponse.setCreated(new Date());
                pendingResponse.setOutcome(ClaimResponse.RemittanceOutcome.QUEUED);
                pendingResponse.setRequest(new Reference("Claim/" + claimId));

                return pendingResponse;
            }

            ClaimResponse response = (ClaimResponse) bundle.getEntryFirstRep().getResource();
            log.info("Found response for claim ID: {}, outcome: {}", claimId, response.getOutcome());
            return response;
        } catch (BaseServerResponseException e) {
            log.error("FHIR server error: {} - {}", e.getStatusCode(), e.getMessage());
            return createErrorResponse("Failed to check status: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error checking prior auth status: {}", e.getMessage(), e);
            return createErrorResponse("Failed to check status: " + e.getMessage());
        }
    }

    /**
     * Cancels a prior authorization request.
     *
     * @param claimId The ID of the prior authorization claim to cancel
     * @return True if cancellation was successful
     */
    public boolean cancelPriorAuth(String claimId) {
        log.info("Cancelling prior authorization with ID: {}", claimId);

        try {
            // First read the existing claim to get all its data
            Claim existingClaim = fhirClient.read()
                    .resource(Claim.class)
                    .withId(claimId)
                    .execute();

            // Update the status to cancelled
            existingClaim.setStatus(Claim.ClaimStatus.CANCELLED);

            // Add a comment about cancellation
            Extension cancelExtension = new Extension();
            cancelExtension.setUrl("http://example.org/fhir/StructureDefinition/claim-cancellation-reason");
            cancelExtension.setValue(new StringType("Cancelled by user on " + new Date()));
            existingClaim.addExtension(cancelExtension);

            // Update the claim
            MethodOutcome outcome = fhirClient.update()
                    .resource(existingClaim)
                    .execute();

            log.info("Successfully cancelled prior authorization: {}, version: {}", claimId,
                    outcome.getId().getVersionIdPart());

            // Create a cancellation response
            ClaimResponse cancelResponse = new ClaimResponse();
            cancelResponse.setId("cancel-" + claimId);
            cancelResponse.setStatus(ClaimResponse.ClaimResponseStatus.ACTIVE);
            cancelResponse.setCreated(new Date());
            cancelResponse.setOutcome(ClaimResponse.RemittanceOutcome.COMPLETE);
            cancelResponse.setDisposition("Cancelled by user");
            cancelResponse.setRequest(new Reference("Claim/" + claimId));

            // Submit the cancellation response
            fhirClient.create().resource(cancelResponse).execute();

            return true;
        } catch (BaseServerResponseException e) {
            log.error("FHIR server error: {} - {}", e.getStatusCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Failed to cancel prior authorization: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Legacy method - use submitPriorAuth instead
     *
     * @return The ID of the submitted prior authorization request
     */
    public String sendPriorAuthRequest() {
        log.info("Using legacy sendPriorAuthRequest method");

        // Create a basic claim
        Claim claim = new Claim();
        claim.setStatus(Claim.ClaimStatus.ACTIVE);
        claim.setUse(Claim.Use.PREAUTHORIZATION);
        claim.setCreated(new Date());

        // Add required references
        claim.setProvider(new Reference().setDisplay("Example Provider"));
        claim.setInsurer(new Reference().setDisplay("Example Insurer"));
        claim.setPatient(new Reference().setDisplay("Example Patient"));

        // Add a type (required by FHIR spec)
        CodeableConcept type = new CodeableConcept();
        type.addCoding()
            .setSystem("http://terminology.hl7.org/CodeSystem/claim-type")
            .setCode("professional")
            .setDisplay("Professional");
        claim.setType(type);

        // Submit the claim
        ClaimResponse response = submitPriorAuth(claim);
        String id = response.getId();
        log.info("Legacy method returned ID: {}", id);
        return id;
    }

    /**
     * Gets the root cause message from an exception
     *
     * @param e The exception to get the root cause from
     * @return The root cause message
     */
    private String getRootCauseMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();
        if (message == null || message.isEmpty()) {
            message = cause.getClass().getSimpleName();
        }

        // Check for common connection issues
        if (cause instanceof java.net.ConnectException) {
            return "Cannot connect to Availity server. Check network and server status.";
        } else if (cause instanceof java.net.UnknownHostException) {
            return "Cannot resolve Availity server hostname. Check network configuration.";
        } else if (cause instanceof java.net.SocketTimeoutException) {
            return "Connection to Availity timed out. Server may be slow or unavailable.";
        } else if (message.contains("401") || message.contains("Unauthorized")) {
            return "Authentication failed. Check client ID and secret.";
        } else if (message.contains("403") || message.contains("Forbidden")) {
            return "Access denied. Check permissions and scope.";
        } else if (message.contains("404") || message.contains("Not Found")) {
            return "Resource not found. Check URL and endpoint configuration.";
        } else if (message.contains("500") || message.contains("Internal Server Error")) {
            return "Availity server error. Try again later.";
        }

        return message;
    }
}
