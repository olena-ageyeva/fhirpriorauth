package com.example.fhirpriorauth.util;

// import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;


@Component
public class FhirToAvailityMapper {

    private static final Logger log = LoggerFactory.getLogger(FhirToAvailityMapper.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // Default values for codes
    @Value("${availity.default.requestTypeCode:HS}")
    private String defaultRequestTypeCode;

    @Value("${availity.default.serviceTypeCode:73}")
    private String defaultServiceTypeCode;

    @Value("${availity.default.placeOfServiceCode:22}")
    private String defaultPlaceOfServiceCode;

    @Value("${availity.default.serviceLevelCode:E}")
    private String defaultServiceLevelCode;

    @Value("${availity.default.quantityTypeCode:VS}")
    private String defaultQuantityTypeCode;

    @Value("${availity.default.procedureQualifierCode:HC}")
    private String defaultProcedureQualifierCode;

    @Value("${availity.default.procedureQuantityTypeCode:UN}")
    private String defaultProcedureQuantityTypeCode;

    @Value("${availity.default.diagnosisQualifierCode:ABK}")
    private String defaultDiagnosisQualifierCode;

    @Value("${availity.default.subscriberRelationshipCode:18}")
    private String defaultSubscriberRelationshipCode;

    @Value("${availity.default.providerRoleCode:1P}")
    private String defaultProviderRoleCode;

    @Value("${availity.default.renderingProviderRoleCode:71}")
    private String defaultRenderingProviderRoleCode;

    @Value("${fhir.system.icd10:http://hl7.org/fhir/sid/icd-10}")
    private String icd10System;

    @Value("${fhir.system.cpt:http://www.ama-assn.org/go/cpt}")
    private String cptSystem;

    // FhirContext is not used directly but might be needed for future enhancements
    // @Autowired(required = false)
    // private FhirContext fhirContext;

    @Autowired(required = false)
    private IGenericClient fhirClient;

    public Map<String, Object> convertFhirToAvailityAPI(Claim claim) {
        log.debug("Converting FHIR Claim to Availity API format");
        Map<String, Object> serviceReview = new HashMap<>();

        // Payer
        Map<String, Object> payer = new HashMap<>();
        if (claim.hasInsurer()) {
            if (claim.getInsurer().hasIdentifier()) {
                payer.put("id", claim.getInsurer().getIdentifier().getValue());
            } else if (claim.getInsurer().hasReference()) {
                payer.put("id", extractIdFromReference(claim.getInsurer().getReference()));
            } else if (claim.getInsurer().hasDisplay()) {
                payer.put("id", claim.getInsurer().getDisplay());
            } else {
                payer.put("id", "unknown");
            }

            if (claim.getInsurer().hasDisplay()) {
                payer.put("name", claim.getInsurer().getDisplay());
            }
        } else {
            payer.put("id", "unknown");
        }
        serviceReview.put("payer", payer);

        // Requesting Provider
        Map<String, Object> provider = new HashMap<>();
        if (claim.hasProvider()) {
            try {
                // Try to fetch the Practitioner resource if FHIR client is available
                if (fhirClient != null) {
                    Practitioner practitioner = fhirClient.read().resource(Practitioner.class)
                            .withUrl(claim.getProvider().getReference()).execute();

                    extractProviderDetails(provider, practitioner);
                } else {
                    // Extract what we can from the Reference
                    extractProviderFromReference(provider, claim.getProvider());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch Practitioner resource: {}", e.getMessage());
                // Extract what we can from the Reference
                extractProviderFromReference(provider, claim.getProvider());
            }
        } else {
            // Set default values
            provider.put("npi", "unknown");
            provider.put("firstName", "unknown");
            provider.put("lastName", "unknown");
        }

        // Always set the role code
        provider.put("roleCode", defaultProviderRoleCode);
        serviceReview.put("requestingProvider", provider);

        // Subscriber and Patient
        Map<String, Object> subscriber = new HashMap<>();
        Map<String, Object> patientMap = new HashMap<>();

        if (claim.hasPatient()) {
            try {
                // Try to fetch the Patient resource if FHIR client is available
                if (fhirClient != null) {
                    Patient patient = fhirClient.read().resource(Patient.class)
                            .withUrl(claim.getPatient().getReference()).execute();

                    extractPatientDetails(subscriber, patientMap, patient);
                } else {
                    // Extract what we can from the Reference
                    extractPatientFromReference(subscriber, patientMap, claim.getPatient());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch Patient resource: {}", e.getMessage());
                // Extract what we can from the Reference
                extractPatientFromReference(subscriber, patientMap, claim.getPatient());
            }
        } else {
            // Set default values
            subscriber.put("memberId", "unknown");
            patientMap.put("firstName", "unknown");
            patientMap.put("lastName", "unknown");
            patientMap.put("birthDate", DATE_FORMAT.format(new Date()));
        }

        // Always set the relationship code
        patientMap.put("subscriberRelationshipCode", defaultSubscriberRelationshipCode);
        serviceReview.put("subscriber", subscriber);
        serviceReview.put("patient", patientMap);

        // Diagnoses
        List<Map<String, String>> diagnoses = new ArrayList<>();
        for (Claim.DiagnosisComponent diag : claim.getDiagnosis()) {
            Map<String, String> diagnosis = new HashMap<>();
            diagnosis.put("qualifierCode", defaultDiagnosisQualifierCode);

            if (diag.hasDiagnosis() && diag.getDiagnosis() instanceof CodeableConcept) {
                CodeableConcept cc = (CodeableConcept) diag.getDiagnosis();
                if (cc.hasCoding()) {
                    Coding coding = cc.getCodingFirstRep();
                    diagnosis.put("code", coding.hasCode() ? coding.getCode() : "unknown");
                } else {
                    diagnosis.put("code", "unknown");
                }
            } else {
                diagnosis.put("code", "unknown");
            }

            diagnoses.add(diagnosis);
        }
        serviceReview.put("diagnoses", diagnoses);

        // Dates and Quantity
        String fromDate = claim.hasCreated() ? DATE_FORMAT.format(claim.getCreated()) : DATE_FORMAT.format(new Date());
        String toDate = fromDate;

        // Try to extract service period from the claim items
        if (claim.hasItem()) {
            for (Claim.ItemComponent item : claim.getItem()) {
                if (item.hasServicedPeriod()) {
                    if (item.getServicedPeriod().hasStart()) {
                        fromDate = DATE_FORMAT.format(item.getServicedPeriod().getStart());
                    }
                    if (item.getServicedPeriod().hasEnd()) {
                        toDate = DATE_FORMAT.format(item.getServicedPeriod().getEnd());
                    }
                    break; // Use the first item with a service period
                }
            }
        }

        serviceReview.put("requestTypeCode", defaultRequestTypeCode);
        serviceReview.put("serviceTypeCode", defaultServiceTypeCode);
        serviceReview.put("placeOfServiceCode", defaultPlaceOfServiceCode);
        serviceReview.put("serviceLevelCode", defaultServiceLevelCode);
        serviceReview.put("fromDate", fromDate);
        serviceReview.put("toDate", toDate);
        serviceReview.put("quantity", "1"); // Default quantity
        serviceReview.put("quantityTypeCode", defaultQuantityTypeCode);

        // Procedures
        List<Map<String, String>> procedures = new ArrayList<>();
        for (Claim.ProcedureComponent proc : claim.getProcedure()) {
            Map<String, String> procedure = new HashMap<>();
            procedure.put("fromDate", fromDate);
            procedure.put("toDate", toDate);

            if (proc.hasProcedure() && proc.getProcedure() instanceof CodeableConcept) {
                CodeableConcept cc = (CodeableConcept) proc.getProcedure();
                if (cc.hasCoding()) {
                    Coding coding = cc.getCodingFirstRep();
                    procedure.put("code", coding.hasCode() ? coding.getCode() : "unknown");
                } else {
                    procedure.put("code", "unknown");
                }
            } else {
                procedure.put("code", "unknown");
            }

            procedure.put("qualifierCode", defaultProcedureQualifierCode);
            procedure.put("quantity", "1"); // Default quantity
            procedure.put("quantityTypeCode", defaultProcedureQuantityTypeCode);
            procedures.add(procedure);
        }
        serviceReview.put("procedures", procedures);

        // Rendering Providers (reuse the requesting provider information)
        List<Map<String, String>> renderingProviders = new ArrayList<>();
        Map<String, String> rendering = new HashMap<>();
        rendering.put("lastName", provider.getOrDefault("lastName", "unknown").toString());
        rendering.put("firstName", provider.getOrDefault("firstName", "unknown").toString());
        rendering.put("npi", provider.getOrDefault("npi", "unknown").toString());
        rendering.put("taxId", provider.getOrDefault("taxId", "unknown").toString());
        rendering.put("roleCode", defaultRenderingProviderRoleCode);
        rendering.put("addressLine1", provider.getOrDefault("addressLine1", "unknown").toString());
        rendering.put("city", provider.getOrDefault("city", "unknown").toString());
        rendering.put("stateCode", provider.getOrDefault("stateCode", "unknown").toString());
        rendering.put("zipCode", provider.getOrDefault("zipCode", "unknown").toString());
        renderingProviders.add(rendering);
        serviceReview.put("renderingProviders", renderingProviders);

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("serviceReview", serviceReview);
        return wrapper;
    }

    /**
     * Extract provider details from a Practitioner resource
     */
    private void extractProviderDetails(Map<String, Object> provider, Practitioner practitioner) {
        provider.put("npi", practitioner.hasIdentifier() ?
                practitioner.getIdentifierFirstRep().getValue() : "unknown");

        if (practitioner.hasName()) {
            HumanName name = practitioner.getNameFirstRep();
            provider.put("firstName", name.hasGiven() ? name.getGivenAsSingleString() : "unknown");
            provider.put("lastName", name.hasFamily() ? name.getFamily() : "unknown");
            provider.put("contactName", name.hasText() ? name.getText() :
                    (name.hasGiven() && name.hasFamily() ?
                    name.getGivenAsSingleString() + " " + name.getFamily() : "unknown"));
        } else {
            provider.put("firstName", "unknown");
            provider.put("lastName", "unknown");
            provider.put("contactName", "unknown");
        }

        if (practitioner.hasAddress()) {
            Address address = practitioner.getAddressFirstRep();
            provider.put("addressLine1", address.hasLine() && !address.getLine().isEmpty() ?
                    address.getLine().get(0).getValue() : "unknown");
            provider.put("city", address.hasCity() ? address.getCity() : "unknown");
            provider.put("stateCode", address.hasState() ? address.getState() : "unknown");
            provider.put("zipCode", address.hasPostalCode() ? address.getPostalCode() : "unknown");
        } else {
            provider.put("addressLine1", "unknown");
            provider.put("city", "unknown");
            provider.put("stateCode", "unknown");
            provider.put("zipCode", "unknown");
        }

        provider.put("phone", practitioner.hasTelecom() ?
                practitioner.getTelecomFirstRep().getValue() : "unknown");
    }

    /**
     * Extract provider information from a Reference
     */
    private void extractProviderFromReference(Map<String, Object> provider, Reference reference) {
        String id = extractIdFromReference(reference.getReference());
        provider.put("npi", id);

        if (reference.hasDisplay()) {
            String display = reference.getDisplay();
            String[] parts = display.split(" ", 2);
            if (parts.length > 1) {
                provider.put("firstName", parts[0]);
                provider.put("lastName", parts[1]);
            } else {
                provider.put("firstName", "unknown");
                provider.put("lastName", display);
            }
            provider.put("contactName", display);
        } else {
            provider.put("firstName", "unknown");
            provider.put("lastName", "unknown");
            provider.put("contactName", "unknown");
        }

        // Default address values
        provider.put("addressLine1", "unknown");
        provider.put("city", "unknown");
        provider.put("stateCode", "unknown");
        provider.put("zipCode", "unknown");
        provider.put("phone", "unknown");
    }

    /**
     * Extract patient details from a Patient resource
     */
    private void extractPatientDetails(Map<String, Object> subscriber, Map<String, Object> patient, Patient patientResource) {
        subscriber.put("memberId", patientResource.hasIdentifier() ?
                patientResource.getIdentifierFirstRep().getValue() : "unknown");

        if (patientResource.hasName()) {
            HumanName name = patientResource.getNameFirstRep();
            String firstName = name.hasGiven() ? name.getGivenAsSingleString() : "unknown";
            String lastName = name.hasFamily() ? name.getFamily() : "unknown";

            patient.put("firstName", firstName);
            patient.put("lastName", lastName);
            subscriber.put("firstName", firstName);
            subscriber.put("lastName", lastName);
        } else {
            patient.put("firstName", "unknown");
            patient.put("lastName", "unknown");
            subscriber.put("firstName", "unknown");
            subscriber.put("lastName", "unknown");
        }

        patient.put("birthDate", patientResource.hasBirthDate() ?
                DATE_FORMAT.format(patientResource.getBirthDate()) : DATE_FORMAT.format(new Date()));
    }

    /**
     * Extract patient information from a Reference
     */
    private void extractPatientFromReference(Map<String, Object> subscriber, Map<String, Object> patient, Reference reference) {
        String id = extractIdFromReference(reference.getReference());
        subscriber.put("memberId", id);

        if (reference.hasDisplay()) {
            String display = reference.getDisplay();
            String[] parts = display.split(" ", 2);
            if (parts.length > 1) {
                String firstName = parts[0];
                String lastName = parts[1];
                patient.put("firstName", firstName);
                patient.put("lastName", lastName);
                subscriber.put("firstName", firstName);
                subscriber.put("lastName", lastName);
            } else {
                patient.put("firstName", "unknown");
                patient.put("lastName", display);
                subscriber.put("firstName", "unknown");
                subscriber.put("lastName", display);
            }
        } else {
            patient.put("firstName", "unknown");
            patient.put("lastName", "unknown");
            subscriber.put("firstName", "unknown");
            subscriber.put("lastName", "unknown");
        }

        patient.put("birthDate", DATE_FORMAT.format(new Date()));
    }

    /**
     * Extract the ID from a FHIR reference
     */
    private String extractIdFromReference(String reference) {
        if (reference == null) {
            return "unknown";
        }

        int lastSlash = reference.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < reference.length() - 1) {
            return reference.substring(lastSlash + 1);
        }

        return reference;
    }

    @SuppressWarnings("unchecked")
    public Claim convertAvailityAPIToFhir(Map<String, Object> availityResponse) {
        log.debug("Converting Availity API format to FHIR Claim");
        Claim claim = new Claim();
        claim.setStatus(Claim.ClaimStatus.ACTIVE);
        claim.setUse(Claim.Use.PREAUTHORIZATION);
        claim.setCreated(new Date());

        // Set ID if available
        if (availityResponse.containsKey("id")) {
            claim.setId(availityResponse.get("id").toString());
        }

        // Extract patient information
        Object patientObj = availityResponse.get("patient");
        if (patientObj instanceof Map) {
            Map<String, Object> patientMap = (Map<String, Object>) patientObj;
            String firstName = getStringValue(patientMap, "firstName", "");
            String lastName = getStringValue(patientMap, "lastName", "");

            // Create patient reference
            Reference patientRef = new Reference();
            patientRef.setReference("Patient/" + (firstName + lastName).replaceAll("\\s+", ""));
            patientRef.setDisplay(firstName + " " + lastName);
            claim.setPatient(patientRef);

            // Set birth date if available
            String birthDateStr = getStringValue(patientMap, "birthDate", null);
            if (birthDateStr != null) {
                try {
                    Date birthDate = DATE_FORMAT.parse(birthDateStr);
                    Extension birthDateExt = new Extension();
                    birthDateExt.setUrl("http://hl7.org/fhir/StructureDefinition/patient-birthDate");
                    birthDateExt.setValue(new DateType(birthDate));
                    claim.addExtension(birthDateExt);
                } catch (Exception e) {
                    log.warn("Failed to parse birth date: {}", birthDateStr);
                }
            }
        }

        // Extract subscriber information
        Object subscriberObj = availityResponse.get("subscriber");
        if (subscriberObj instanceof Map) {
            Map<String, Object> subscriberMap = (Map<String, Object>) subscriberObj;
            String memberId = getStringValue(subscriberMap, "memberId", null);

            if (memberId != null) {
                // Create coverage reference
                Reference coverageRef = new Reference();
                coverageRef.setReference("Coverage/" + memberId);
                coverageRef.setDisplay("Coverage ID: " + memberId);

                // Add insurance component
                Claim.InsuranceComponent insurance = new Claim.InsuranceComponent();
                insurance.setSequence(1);
                insurance.setFocal(true);
                insurance.setCoverage(coverageRef);
                claim.addInsurance(insurance);
            }
        }

        // Extract payer information
        Object payerObj = availityResponse.get("payer");
        if (payerObj instanceof Map) {
            Map<String, Object> payerMap = (Map<String, Object>) payerObj;
            String payerId = getStringValue(payerMap, "id", null);
            String payerName = getStringValue(payerMap, "name", null);

            if (payerId != null) {
                // Create insurer reference
                Reference insurerRef = new Reference();
                insurerRef.setReference("Organization/" + payerId);
                if (payerName != null) {
                    insurerRef.setDisplay(payerName);
                }
                claim.setInsurer(insurerRef);
            }
        }

        // Extract diagnoses
        Object diagnosesObj = availityResponse.get("diagnoses");
        if (diagnosesObj instanceof List) {
            List<?> diagnosesList = (List<?>) diagnosesObj;
            int sequence = 1;

            for (Object diagObj : diagnosesList) {
                if (diagObj instanceof Map) {
                    Map<?, ?> diagMap = (Map<?, ?>) diagObj;
                    String code = getMapStringValue(diagMap, "code", null);

                    if (code != null) {
                        Claim.DiagnosisComponent diag = new Claim.DiagnosisComponent();
                        diag.setSequence(sequence++);

                        CodeableConcept diagConcept = new CodeableConcept();
                        Coding coding = new Coding();
                        coding.setSystem(icd10System);
                        coding.setCode(code);
                        diagConcept.addCoding(coding);
                        diag.setDiagnosis(diagConcept);

                        claim.addDiagnosis(diag);
                    }
                }
            }
        }

        // Extract procedures and create items
        Object proceduresObj = availityResponse.get("procedures");
        if (proceduresObj instanceof List) {
            List<?> proceduresList = (List<?>) proceduresObj;
            int sequence = 1;

            for (Object procObj : proceduresList) {
                if (procObj instanceof Map) {
                    Map<?, ?> procMap = (Map<?, ?>) procObj;
                    String code = getMapStringValue(procMap, "code", null);
                    String fromDateStr = getMapStringValue(procMap, "fromDate", null);
                    String toDateStr = getMapStringValue(procMap, "toDate", null);
                    String quantityStr = getMapStringValue(procMap, "quantity", "1");

                    if (code != null) {
                        // Add procedure component
                        Claim.ProcedureComponent proc = new Claim.ProcedureComponent();
                        proc.setSequence(sequence);

                        // Set procedure date if available
                        if (fromDateStr != null) {
                            try {
                                Date procDate = DATE_FORMAT.parse(fromDateStr);
                                proc.setDate(procDate);
                            } catch (Exception e) {
                                log.warn("Failed to parse procedure date: {}", fromDateStr);
                                proc.setDate(new Date());
                            }
                        } else {
                            proc.setDate(new Date());
                        }

                        // Set procedure code
                        CodeableConcept procConcept = new CodeableConcept();
                        Coding coding = new Coding();
                        coding.setSystem(cptSystem);
                        coding.setCode(code);
                        procConcept.addCoding(coding);
                        proc.setProcedure(procConcept);

                        claim.addProcedure(proc);

                        // Add corresponding item
                        Claim.ItemComponent item = new Claim.ItemComponent();
                        item.setSequence(sequence++);

                        // Set product or service
                        item.setProductOrService(procConcept);

                        // Set serviced period if available
                        if (fromDateStr != null || toDateStr != null) {
                            Period period = new Period();

                            if (fromDateStr != null) {
                                try {
                                    period.setStart(DATE_FORMAT.parse(fromDateStr));
                                } catch (Exception e) {
                                    log.warn("Failed to parse from date: {}", fromDateStr);
                                }
                            }

                            if (toDateStr != null) {
                                try {
                                    period.setEnd(DATE_FORMAT.parse(toDateStr));
                                } catch (Exception e) {
                                    log.warn("Failed to parse to date: {}", toDateStr);
                                }
                            }

                            item.setServiced(period);
                        }

                        // Set quantity if available
                        try {
                            int quantity = Integer.parseInt(quantityStr);
                            item.setQuantity(new SimpleQuantity().setValue(quantity));
                        } catch (Exception e) {
                            log.warn("Failed to parse quantity: {}", quantityStr);
                        }

                        claim.addItem(item);
                    }
                }
            }
        }

        return claim;
    }

    /**
     * Get a string value from a map, with a default value if not found or not a string
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }

    /**
     * Get a string value from a map with unknown key types
     */
    private String getMapStringValue(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }
}
