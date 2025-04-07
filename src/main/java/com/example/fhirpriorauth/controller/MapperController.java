package com.example.fhirpriorauth.controller;

import com.example.fhirpriorauth.util.FhirToAvailityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.Claim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for mapping between FHIR and Availity formats
 */
@RestController
@RequestMapping("/api/mapper")
public class MapperController {

    private static final Logger log = LoggerFactory.getLogger(MapperController.class);
    private final FhirToAvailityMapper mapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public MapperController(FhirToAvailityMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Convert FHIR Claim to Availity format
     *
     * @param fhirJson FHIR Claim resource as JSON
     * @return Availity format JSON
     */
    @PostMapping("/fhir-to-availity")
    public ResponseEntity<?> convertFhirToAvaility(@RequestBody Map<String, Object> fhirMap) {
        try {
            log.info("Converting FHIR to Availity format");

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
            provider.put("lastName", fhirMap.containsKey("provider") ?
                ((Map<String, Object>)fhirMap.get("provider")).getOrDefault("display", "PROVIDER") : "PROVIDER");
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
            String patientDisplay = "";
            if (fhirMap.containsKey("patient") && ((Map<String, Object>)fhirMap.get("patient")).containsKey("display")) {
                patientDisplay = (String)((Map<String, Object>)fhirMap.get("patient")).get("display");
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
            if (fhirMap.containsKey("diagnosis")) {
                List<Map<String, Object>> diagList = (List<Map<String, Object>>) fhirMap.get("diagnosis");
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
            if (fhirMap.containsKey("procedure")) {
                List<Map<String, Object>> procList = (List<Map<String, Object>>) fhirMap.get("procedure");
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

            return ResponseEntity.ok(wrapper);
        } catch (Exception e) {
            log.error("Error converting FHIR to Availity", e);
            return ResponseEntity.badRequest().body("Error converting FHIR to Availity: " + e.getMessage());
        }
    }

    /**
     * Convert Availity format to FHIR Claim
     *
     * @param availityJson Availity format JSON
     * @return FHIR Claim resource as JSON
     */
    @PostMapping("/availity-to-fhir")
    public ResponseEntity<?> convertAvailityToFhir(@RequestBody Map<String, Object> availityMap) {
        try {
            log.info("Converting Availity to FHIR format");

            // Extract serviceReview if it exists
            Map<String, Object> serviceReview = availityMap;
            if (availityMap.containsKey("serviceReview")) {
                serviceReview = (Map<String, Object>) availityMap.get("serviceReview");
            }

            // Create a FHIR Claim resource
            Map<String, Object> fhirClaim = new HashMap<>();
            fhirClaim.put("resourceType", "Claim");
            fhirClaim.put("id", "example-claim-" + Math.floor(Math.random() * 10000));
            fhirClaim.put("status", "active");
            fhirClaim.put("use", "preauthorization");
            fhirClaim.put("created", java.time.LocalDate.now().toString());

            // Patient information
            Map<String, Object> patient = new HashMap<>();
            String patientLastName = "PATIENT";
            String patientFirstName = "TEST";

            if (serviceReview.containsKey("patient")) {
                Map<String, Object> patientInfo = (Map<String, Object>) serviceReview.get("patient");
                if (patientInfo.containsKey("lastName")) {
                    patientLastName = (String) patientInfo.get("lastName");
                }
                if (patientInfo.containsKey("firstName")) {
                    patientFirstName = (String) patientInfo.get("firstName");
                }
            }

            patient.put("reference", "Patient/" + patientLastName);
            patient.put("display", patientFirstName + " " + patientLastName);
            fhirClaim.put("patient", patient);

            // Provider information
            Map<String, Object> provider = new HashMap<>();
            String providerNpi = "1234567893";
            String providerName = "PROVIDER";

            if (serviceReview.containsKey("requestingProvider")) {
                Map<String, Object> providerInfo = (Map<String, Object>) serviceReview.get("requestingProvider");
                if (providerInfo.containsKey("npi")) {
                    providerNpi = (String) providerInfo.get("npi");
                }
                if (providerInfo.containsKey("lastName")) {
                    providerName = (String) providerInfo.get("lastName");
                }
            }

            provider.put("reference", "Organization/" + providerNpi);
            provider.put("display", providerName);
            fhirClaim.put("provider", provider);

            // Insurer information
            Map<String, Object> insurer = new HashMap<>();
            String payerId = "BCBSF";
            String payerName = "FLORIDA BLUE";

            if (serviceReview.containsKey("payer")) {
                Map<String, Object> payerInfo = (Map<String, Object>) serviceReview.get("payer");
                if (payerInfo.containsKey("id")) {
                    payerId = (String) payerInfo.get("id");
                }
                if (payerInfo.containsKey("name")) {
                    payerName = (String) payerInfo.get("name");
                }
            }

            insurer.put("reference", "Organization/" + payerId);
            insurer.put("display", payerName);
            fhirClaim.put("insurer", insurer);

            // Diagnoses
            List<Map<String, Object>> diagnosisList = new ArrayList<>();

            if (serviceReview.containsKey("diagnoses")) {
                List<Map<String, Object>> diagnoses = (List<Map<String, Object>>) serviceReview.get("diagnoses");
                int sequence = 1;

                for (Map<String, Object> diag : diagnoses) {
                    Map<String, Object> diagnosis = new HashMap<>();
                    diagnosis.put("sequence", sequence++);

                    Map<String, Object> diagnosisConcept = new HashMap<>();
                    List<Map<String, Object>> coding = new ArrayList<>();
                    Map<String, Object> code = new HashMap<>();

                    code.put("system", "http://hl7.org/fhir/sid/icd-10");
                    code.put("code", diag.containsKey("code") ? diag.get("code") : "78900");
                    code.put("display", "Diagnosis " + (sequence - 1));

                    coding.add(code);
                    diagnosisConcept.put("coding", coding);
                    diagnosis.put("diagnosisCodeableConcept", diagnosisConcept);

                    diagnosisList.add(diagnosis);
                }
            }

            fhirClaim.put("diagnosis", diagnosisList);

            // Procedures
            List<Map<String, Object>> procedureList = new ArrayList<>();

            if (serviceReview.containsKey("procedures")) {
                List<Map<String, Object>> procedures = (List<Map<String, Object>>) serviceReview.get("procedures");
                int sequence = 1;

                for (Map<String, Object> proc : procedures) {
                    Map<String, Object> procedure = new HashMap<>();
                    procedure.put("sequence", sequence++);

                    Map<String, Object> procedureConcept = new HashMap<>();
                    List<Map<String, Object>> coding = new ArrayList<>();
                    Map<String, Object> code = new HashMap<>();

                    code.put("system", "http://www.ama-assn.org/go/cpt");
                    code.put("code", proc.containsKey("code") ? proc.get("code") : "99213");
                    code.put("display", "Procedure " + (sequence - 1));

                    coding.add(code);
                    procedureConcept.put("coding", coding);
                    procedure.put("procedureCodeableConcept", procedureConcept);
                    procedure.put("date", proc.containsKey("fromDate") ? proc.get("fromDate") : "2022-09-02");

                    procedureList.add(procedure);
                }
            }

            fhirClaim.put("procedure", procedureList);

            // Insurance
            List<Map<String, Object>> insuranceList = new ArrayList<>();
            Map<String, Object> insurance = new HashMap<>();
            insurance.put("sequence", 1);
            insurance.put("focal", true);

            Map<String, Object> coverage = new HashMap<>();
            String memberId = "ASBA1274712";
            String lastName = "PATIENT";

            if (serviceReview.containsKey("subscriber")) {
                Map<String, Object> subscriberInfo = (Map<String, Object>) serviceReview.get("subscriber");
                if (subscriberInfo.containsKey("memberId")) {
                    memberId = (String) subscriberInfo.get("memberId");
                }
                if (subscriberInfo.containsKey("lastName")) {
                    lastName = (String) subscriberInfo.get("lastName");
                }
            }

            coverage.put("reference", "Coverage/" + memberId);
            coverage.put("display", "Coverage for " + lastName);
            insurance.put("coverage", coverage);

            insuranceList.add(insurance);
            fhirClaim.put("insurance", insuranceList);

            // Items
            List<Map<String, Object>> itemList = new ArrayList<>();

            if (serviceReview.containsKey("procedures")) {
                List<Map<String, Object>> procedures = (List<Map<String, Object>>) serviceReview.get("procedures");
                int sequence = 1;

                for (Map<String, Object> proc : procedures) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("sequence", sequence++);

                    Map<String, Object> productOrService = new HashMap<>();
                    List<Map<String, Object>> coding = new ArrayList<>();
                    Map<String, Object> code = new HashMap<>();

                    code.put("system", "http://www.ama-assn.org/go/cpt");
                    code.put("code", proc.containsKey("code") ? proc.get("code") : "99213");
                    code.put("display", "Service " + (sequence - 1));

                    coding.add(code);
                    productOrService.put("coding", coding);
                    item.put("productOrService", productOrService);

                    Map<String, Object> servicedPeriod = new HashMap<>();
                    servicedPeriod.put("start", proc.containsKey("fromDate") ? proc.get("fromDate") : "2022-09-02");
                    servicedPeriod.put("end", proc.containsKey("toDate") ? proc.get("toDate") : "2022-09-13");
                    item.put("servicedPeriod", servicedPeriod);

                    Map<String, Object> quantity = new HashMap<>();
                    quantity.put("value", proc.containsKey("quantity") ? Integer.parseInt((String)proc.get("quantity")) : 1);
                    item.put("quantity", quantity);

                    itemList.add(item);
                }
            }

            fhirClaim.put("item", itemList);

            return ResponseEntity.ok(fhirClaim);
        } catch (Exception e) {
            log.error("Error converting Availity to FHIR", e);
            return ResponseEntity.badRequest().body("Error converting Availity to FHIR: " + e.getMessage());
        }
    }
}
