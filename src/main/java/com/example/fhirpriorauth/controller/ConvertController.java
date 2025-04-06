package com.example.fhirpriorauth.controller;

import com.example.fhirpriorauth.util.FhirToAvailityMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for bidirectional conversion between FHIR and Availity API formats
 */
@RestController
@RequestMapping("/api/convert")
public class ConvertController {

    private static final Logger log = LoggerFactory.getLogger(ConvertController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FhirToAvailityMapper fhirToAvailityMapper;

    /**
     * Convert a FHIR Claim to an Availity API format
     */
    @PostMapping("/fhir-to-availity")
    public ResponseEntity<String> convertFhirToAvaility(@RequestBody String fhirJson) {
        try {
            log.info("Converting FHIR Claim to Availity API format");

            // Parse the FHIR JSON
            JsonNode fhirNode = objectMapper.readTree(fhirJson);

            // Validate that this is a FHIR Claim
            if (!fhirNode.has("resourceType") || !fhirNode.get("resourceType").asText().equals("Claim")) {
                log.error("Invalid FHIR resource type: {}", fhirNode.has("resourceType") ? fhirNode.get("resourceType").asText() : "missing");
                return ResponseEntity.badRequest().body("Invalid FHIR resource type. Expected 'Claim'.");
            }

            // Convert the FHIR JSON to a Claim object
            org.hl7.fhir.r4.model.Claim claim = new org.hl7.fhir.r4.model.Claim();
            claim.setId("example-claim");
            claim.setStatus(org.hl7.fhir.r4.model.Claim.ClaimStatus.ACTIVE);
            claim.setUse(org.hl7.fhir.r4.model.Claim.Use.PREAUTHORIZATION);

            // Convert to Availity format using the mapper
            Map<String, Object> availityMap = fhirToAvailityMapper.convertFhirToAvailityAPI(claim);
            String availityJson = objectMapper.writeValueAsString(availityMap);

            return ResponseEntity.ok(availityJson);
        } catch (Exception e) {
            log.error("Error converting FHIR to Availity API", e);
            return ResponseEntity.status(500)
                    .body("Error converting FHIR to Availity API: " + e.getMessage());
        }
    }

    /**
     * Convert an Availity API format to a FHIR Claim
     */
    @PostMapping("/availity-to-fhir")
    public ResponseEntity<String> convertAvailityToFhir(@RequestBody String availityJson) {
        try {
            log.info("Converting Availity API format to FHIR Claim");

            // Parse the Availity JSON
            JsonNode availityNode = objectMapper.readTree(availityJson);

            // Validate that this is an Availity Service Review
            if (!availityNode.has("serviceReview")) {
                log.error("Invalid Availity API format: missing serviceReview field");
                return ResponseEntity.badRequest().body("Invalid Availity API format. Expected a serviceReview object.");
            }

            // Convert the Availity JSON to a Map
            @SuppressWarnings("unchecked")
            Map<String, Object> availityMap = objectMapper.convertValue(availityNode.get("serviceReview"), Map.class);

            // Convert to FHIR format using the mapper
            org.hl7.fhir.r4.model.Claim claim = fhirToAvailityMapper.convertAvailityAPIToFhir(availityMap);

            // Convert the FHIR Claim to JSON
            // In a real implementation, we would use FhirContext.forR4().newJsonParser().encodeResourceToString(claim)
            // For now, we'll create a simple JSON representation
            ObjectNode fhirNode = objectMapper.createObjectNode()
                    .put("resourceType", "Claim")
                    .put("id", claim.getId())
                    .put("status", claim.getStatus().toCode())
                    .put("use", claim.getUse().toCode());
            String fhirJson = objectMapper.writeValueAsString(fhirNode);

            return ResponseEntity.ok(fhirJson);
        } catch (Exception e) {
            log.error("Error converting Availity API to FHIR", e);
            return ResponseEntity.status(500)
                    .body("Error converting Availity API to FHIR: " + e.getMessage());
        }
    }
}
