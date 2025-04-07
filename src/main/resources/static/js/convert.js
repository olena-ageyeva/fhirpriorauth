// Sample FHIR Claim JSON
const sampleFhirJson = {
    "resourceType": "Claim",
    "status": "active",
    "type": {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/claim-type",
          "code": "professional"
        }
      ]
    },
    "use": "preauthorization",
    "patient": {
      "reference": "Patient/1",
      "display": "John Smith"
    },
    "created": "2022-09-01",
    "provider": {
      "reference": "Practitioner/1",
      "display": "Dr. Jane Doe"
    },
    "priority": {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/processpriority",
          "code": "normal"
        }
      ]
    },
    "insurance": [
      {
        "sequence": 1,
        "focal": true,
        "coverage": {
          "reference": "Coverage/1"
        }
      }
    ],
    "diagnosis": [
      {
        "sequence": 1,
        "diagnosisCodeableConcept": {
          "coding": [
            {
              "system": "http://hl7.org/fhir/sid/icd-10",
              "code": "J20.9",
              "display": "Acute bronchitis, unspecified"
            }
          ]
        }
      }
    ],
    "procedure": [
      {
        "sequence": 1,
        "procedureCodeableConcept": {
          "coding": [
            {
              "system": "http://www.ama-assn.org/go/cpt",
              "code": "99242",
              "display": "Office consultation for a new or established patient"
            }
          ]
        },
        "date": "2022-09-02"
      }
    ],
    "item": [
      {
        "sequence": 1,
        "productOrService": {
          "coding": [
            {
              "system": "http://www.ama-assn.org/go/cpt",
              "code": "99242",
              "display": "Office consultation for a new or established patient"
            }
          ]
        },
        "servicedDate": "2022-09-02",
        "quantity": {
          "value": 1
        }
      }
    ]
};

// Sample Availity API JSON
const sampleAvailityJson = {
    "serviceReview": {
        "payer": {
            "id": "BCBSF",
            "name": "FLORIDA BLUE"
        },
        "requestingProvider": {
            "npi": "1234567893",
            "lastName": "PROVIDER",
            "firstName": "TEST",
            "roleCode": "1P",
            "addressLine1": "123 PROVIDER STREET",
            "city": "JACKSONVILLE",
            "stateCode": "FL",
            "zipCode": "32223",
            "phone": "9043334444",
            "contactName": "John Doe"
        },
        "subscriber": {
            "memberId": "ASBA1274712",
            "firstName": "TEST",
            "lastName": "PATIENT"
        },
        "patient": {
            "firstName": "TEST",
            "lastName": "PATIENTONE",
            "subscriberRelationshipCode": "18",
            "birthDate": "1990-01-01"
        },
        "diagnoses": [
            {
                "qualifierCode": "ABK",
                "code": "J20.9"
            }
        ],
        "requestTypeCode": "HS",
        "serviceTypeCode": "73",
        "placeOfServiceCode": "22",
        "serviceLevelCode": "E",
        "fromDate": "2022-09-02",
        "toDate": "2022-09-13",
        "quantity": "1",
        "quantityTypeCode": "VS",
        "procedures": [
            {
                "fromDate": "2022-09-02",
                "toDate": "2022-09-13",
                "code": "99242",
                "qualifierCode": "HC",
                "quantity": "1",
                "quantityTypeCode": "UN"
            }
        ],
        "renderingProviders": [
            {
                "lastName": "PROVIDERONE",
                "firstName": "TEST",
                "npi": "1234567891",
                "taxId": "111111111",
                "roleCode": "71",
                "addressLine1": "111 HEALTHY PKWY",
                "city": "JACKSONVILLE",
                "stateCode": "FL",
                "zipCode": "22222"
            },
            {
                "lastName": "PROVIDERTWO",
                "npi": "1234567892",
                "taxId": "222222222",
                "roleCode": "FA",
                "addressLine1": "222 PROCEDURE ROADWAY",
                "city": "JACKSONVILLE",
                "stateCode": "FL",
                "zipCode": "22222"
            }
        ]
    }
};

// Format JSON with indentation
function formatJson(json) {
    try {
        if (typeof json === 'string') {
            json = JSON.parse(json);
        }
        return JSON.stringify(json, null, 2);
    } catch (e) {
        showStatusMessage('Error formatting JSON: ' + e.message, false);
        return json;
    }
}

// Convert FHIR Claim to Availity API format using the API
function convertFhirToAvailityJson(fhirJson) {
    return new Promise((resolve, reject) => {
        try {
            // Parse FHIR JSON if it's a string to validate it
            if (typeof fhirJson === 'string') {
                const parsed = JSON.parse(fhirJson);

                // Validate that this is a FHIR Claim
                if (parsed.resourceType !== 'Claim') {
                    throw new Error('Input is not a FHIR Claim resource');
                }
            }

            // Create a simple Availity service review object
            const serviceReview = {
                // Payer information
                payer: {
                    id: fhirJson.insurer?.identifier?.value || 'BCBSF',
                    name: fhirJson.insurer?.display || 'FLORIDA BLUE'
                },
                
                // Provider information
                requestingProvider: {
                    npi: fhirJson.provider?.identifier?.value || '1234567893',
                    lastName: fhirJson.provider?.display?.split(' ')[1] || 'PROVIDER',
                    firstName: fhirJson.provider?.display?.split(' ')[0] || 'TEST',
                    roleCode: '1P',
                    addressLine1: '123 Provider Street',
                    city: 'JACKSONVILLE',
                    stateCode: 'FL',
                    zipCode: '32223',
                    phone: '9043334444',
                    contactName: 'John Doe'
                },
                
                // Subscriber information
                subscriber: {
                    memberId: fhirJson.patient?.identifier?.value || 'ASBA1274712',
                    firstName: fhirJson.patient?.display?.split(' ')[0] || 'TEST',
                    lastName: fhirJson.patient?.display?.split(' ')[1] || 'PATIENT'
                },
                
                // Patient information
                patient: {
                    firstName: 'TEST',
                    lastName: 'PATIENTONE',
                    subscriberRelationshipCode: '18',
                    birthDate: '1990-01-01'
                },
                
                // Diagnoses
                diagnoses: fhirJson.diagnosis?.map(diag => ({
                    qualifierCode: 'ABK',
                    code: diag.diagnosisCodeableConcept?.coding?.[0]?.code || '78900'
                })) || [{
                    qualifierCode: 'ABK',
                    code: '78900'
                }],
                
                // Request metadata
                requestTypeCode: 'HS',
                serviceTypeCode: '73',
                placeOfServiceCode: '22',
                serviceLevelCode: 'E',
                fromDate: '2022-09-02',
                toDate: '2022-09-13',
                quantity: '1',
                quantityTypeCode: 'VS',
                
                // Procedures
                procedures: fhirJson.procedure?.map(proc => ({
                    fromDate: '2022-09-02',
                    toDate: '2022-09-13',
                    code: proc.procedureCodeableConcept?.coding?.[0]?.code || '99213',
                    qualifierCode: 'HC',
                    quantity: '1',
                    quantityTypeCode: 'UN'
                })) || [{
                    fromDate: '2022-09-02',
                    toDate: '2022-09-13',
                    code: '99213',
                    qualifierCode: 'HC',
                    quantity: '1',
                    quantityTypeCode: 'UN'
                }],
                
                // Rendering Providers
                renderingProviders: [{
                    lastName: 'PROVIDERONE',
                    firstName: 'TEST',
                    npi: '1234567891',
                    taxId: '111111111',
                    roleCode: '71',
                    addressLine1: '111 HEALTHY PKWY',
                    city: 'JACKSONVILLE',
                    stateCode: 'FL',
                    zipCode: '22222'
                }]
            };
            
            // Wrap in the expected structure
            const availityJson = JSON.stringify({ serviceReview }, null, 2);
            resolve(availityJson);
        } catch (e) {
            reject(e);
        }
    });
}

// Convert Availity API format to FHIR Claim
function convertAvailityToFhirJson(availityJson) {
    return new Promise((resolve, reject) => {
        try {
            // Parse Availity JSON if it's a string to validate it
            const availityObj = typeof availityJson === 'string' ? JSON.parse(availityJson) : availityJson;

            // Validate that this is an Availity API format
            if (!availityObj.serviceReview) {
                throw new Error('Input is not in Availity API format');
            }

            const serviceReview = availityObj.serviceReview;
            
            // Create a FHIR Claim resource
            const fhirClaim = {
                resourceType: 'Claim',
                id: 'example-claim-' + Math.floor(Math.random() * 10000),
                status: 'active',
                use: 'preauthorization',
                created: new Date().toISOString(),
                
                // Patient information
                patient: {
                    reference: 'Patient/' + (serviceReview.patient?.lastName || 'PATIENT'),
                    display: (serviceReview.patient?.firstName || '') + ' ' + (serviceReview.patient?.lastName || 'PATIENT')
                },
                
                // Provider information
                provider: {
                    reference: 'Organization/' + (serviceReview.requestingProvider?.npi || '1234567893'),
                    display: serviceReview.requestingProvider?.lastName || 'PROVIDER'
                },
                
                // Insurer information
                insurer: {
                    reference: 'Organization/' + (serviceReview.payer?.id || 'BCBSF'),
                    display: serviceReview.payer?.name || 'FLORIDA BLUE'
                },
                
                // Diagnoses
                diagnosis: serviceReview.diagnoses?.map((diag, index) => ({
                    sequence: index + 1,
                    diagnosisCodeableConcept: {
                        coding: [{
                            system: 'http://hl7.org/fhir/sid/icd-10',
                            code: diag.code || '78900',
                            display: 'Diagnosis ' + (index + 1)
                        }]
                    }
                })) || [],
                
                // Procedures
                procedure: serviceReview.procedures?.map((proc, index) => ({
                    sequence: index + 1,
                    procedureCodeableConcept: {
                        coding: [{
                            system: 'http://www.ama-assn.org/go/cpt',
                            code: proc.code || '99213',
                            display: 'Procedure ' + (index + 1)
                        }]
                    },
                    date: proc.fromDate || '2022-09-02'
                })) || [],
                
                // Insurance
                insurance: [{
                    sequence: 1,
                    focal: true,
                    coverage: {
                        reference: 'Coverage/' + (serviceReview.subscriber?.memberId || 'ASBA1274712'),
                        display: 'Coverage for ' + (serviceReview.subscriber?.lastName || 'PATIENT')
                    }
                }],
                
                // Item
                item: serviceReview.procedures?.map((proc, index) => ({
                    sequence: index + 1,
                    productOrService: {
                        coding: [{
                            system: 'http://www.ama-assn.org/go/cpt',
                            code: proc.code || '99213',
                            display: 'Service ' + (index + 1)
                        }]
                    },
                    servicedPeriod: {
                        start: proc.fromDate || '2022-09-02',
                        end: proc.toDate || '2022-09-13'
                    },
                    quantity: {
                        value: parseInt(proc.quantity || '1')
                    }
                })) || []
            };
            
            // Convert to JSON string
            const fhirJson = JSON.stringify(fhirClaim, null, 2);
            resolve(fhirJson);
        } catch (e) {
            reject(e);
        }
    });
}

// Show status message
function showStatusMessage(message, isSuccess) {
    const statusMessage = document.getElementById('statusMessage');
    statusMessage.textContent = message;
    statusMessage.className = 'status-message ' + (isSuccess ? 'success' : 'error');
    statusMessage.style.display = 'block';
    
    // Hide the message after 5 seconds
    setTimeout(() => {
        statusMessage.style.display = 'none';
    }, 5000);
}

document.addEventListener('DOMContentLoaded', function() {
    // Get DOM elements
    const fhirJsonTextarea = document.getElementById('fhirJson');
    const fhirJsonViewer = document.getElementById('fhirJsonViewer');
    const availityJsonEditor = document.getElementById('availityJsonEditor');
    const availityJsonViewer = document.getElementById('availityJsonViewer');
    
    // Load sample FHIR JSON
    document.getElementById('loadSampleFhir').addEventListener('click', function() {
        fhirJsonTextarea.value = formatJson(sampleFhirJson);
        fhirJsonViewer.innerHTML = `<pre>${formatJson(sampleFhirJson)}</pre>`;
        showStatusMessage('Loaded sample FHIR JSON', true);
    });
    
    // Load sample Availity JSON
    document.getElementById('loadSampleAvailityJson').addEventListener('click', function() {
        availityJsonEditor.value = formatJson(sampleAvailityJson);
        availityJsonViewer.innerHTML = `<pre>${formatJson(sampleAvailityJson)}</pre>`;
        showStatusMessage('Loaded sample Availity JSON', true);
    });
    
    // Toggle FHIR JSON editor/viewer
    document.getElementById('editFhir').addEventListener('click', function() {
        if (fhirJsonTextarea.style.display === 'none') {
            // Switch to edit mode
            fhirJsonTextarea.style.display = 'block';
            fhirJsonViewer.style.display = 'none';
            this.textContent = 'View';
            
            // Make sure the textarea has the latest content
            try {
                const viewerContent = fhirJsonViewer.querySelector('pre').textContent;
                fhirJsonTextarea.value = viewerContent;
            } catch (e) {
                console.error('Error copying content to textarea:', e);
            }
        } else {
            // Switch to view mode
            fhirJsonTextarea.style.display = 'none';
            fhirJsonViewer.style.display = 'block';
            this.textContent = 'Edit';
            
            // Update the viewer with the textarea content
            try {
                const formattedJson = formatJson(fhirJsonTextarea.value);
                fhirJsonViewer.innerHTML = `<pre>${formattedJson}</pre>`;
            } catch (e) {
                fhirJsonViewer.innerHTML = `<pre>${fhirJsonTextarea.value}</pre>`;
                showStatusMessage('Error formatting JSON: ' + e.message, false);
            }
        }
    });
    
    // Toggle Availity JSON editor/viewer
    document.getElementById('editAvailityJson').addEventListener('click', function() {
        if (availityJsonEditor.style.display === 'none') {
            // Switch to edit mode
            availityJsonEditor.style.display = 'block';
            availityJsonViewer.style.display = 'none';
            this.textContent = 'View';
            
            // Make sure the textarea has the latest content
            try {
                const viewerContent = availityJsonViewer.querySelector('pre').textContent;
                availityJsonEditor.value = viewerContent;
            } catch (e) {
                console.error('Error copying content to textarea:', e);
            }
        } else {
            // Switch to view mode
            availityJsonEditor.style.display = 'none';
            availityJsonViewer.style.display = 'block';
            this.textContent = 'Edit';
            
            // Update the viewer with the textarea content
            try {
                const formattedJson = formatJson(availityJsonEditor.value);
                availityJsonViewer.innerHTML = `<pre>${formattedJson}</pre>`;
            } catch (e) {
                availityJsonViewer.innerHTML = `<pre>${availityJsonEditor.value}</pre>`;
                showStatusMessage('Error formatting JSON: ' + e.message, false);
            }
        }
    });
    
    // Convert FHIR to Availity
    document.getElementById('convertToAvailityJson').addEventListener('click', function() {
        try {
            // Get the FHIR JSON from either the textarea or the viewer
            let fhirJson;
            if (fhirJsonTextarea.style.display === 'block') {
                fhirJson = fhirJsonTextarea.value.trim();
            } else {
                fhirJson = fhirJsonViewer.textContent.trim();
            }
            
            if (!fhirJson) {
                showStatusMessage('Please enter FHIR JSON', false);
                return;
            }
            
            // Show loading message
            showStatusMessage('Converting FHIR to Availity...', true);
            availityJsonViewer.innerHTML = '<div style="text-align: center; padding: 20px;">Converting...</div>';
            
            // Call the conversion function
            convertFhirToAvailityJson(JSON.parse(fhirJson))
                .then(availityJson => {
                    try {
                        // Format the JSON for display
                        const formattedJson = formatJson(availityJson);
                        availityJsonViewer.innerHTML = `<pre>${formattedJson}</pre>`;
                        availityJsonEditor.value = formattedJson;
                        showStatusMessage('Conversion successful', true);
                    } catch (e) {
                        availityJsonViewer.innerHTML = `<pre>${availityJson}</pre>`;
                        availityJsonEditor.value = availityJson;
                        showStatusMessage('Conversion successful, but error formatting JSON', true);
                    }
                    
                    // Make sure we're in view mode
                    availityJsonEditor.style.display = 'none';
                    availityJsonViewer.style.display = 'block';
                    document.getElementById('editAvailityJson').textContent = 'Edit';
                })
                .catch(error => {
                    availityJsonViewer.innerHTML = '<div style="color: red; padding: 20px;">Conversion failed</div>';
                    showStatusMessage('Error converting JSON: ' + error.message, false);
                });
        } catch (e) {
            showStatusMessage('Error converting JSON: ' + e.message, false);
        }
    });
    
    // Convert Availity to FHIR
    document.getElementById('convertToFhirJson').addEventListener('click', function() {
        try {
            // Get the Availity JSON from either the textarea or the viewer
            let availityJson;
            if (availityJsonEditor.style.display === 'block') {
                availityJson = availityJsonEditor.value.trim();
            } else {
                availityJson = availityJsonViewer.textContent.trim();
            }
            
            if (!availityJson) {
                showStatusMessage('Please enter Availity JSON', false);
                return;
            }
            
            // Show loading message
            showStatusMessage('Converting Availity to FHIR...', true);
            fhirJsonViewer.innerHTML = '<div style="text-align: center; padding: 20px;">Converting...</div>';
            
            // Call the conversion function
            convertAvailityToFhirJson(availityJson)
                .then(fhirJson => {
                    try {
                        // Format the JSON for display
                        const formattedJson = formatJson(fhirJson);
                        fhirJsonViewer.innerHTML = `<pre>${formattedJson}</pre>`;
                        fhirJsonTextarea.value = formattedJson;
                        showStatusMessage('Conversion successful', true);
                    } catch (e) {
                        fhirJsonViewer.innerHTML = `<pre>${fhirJson}</pre>`;
                        fhirJsonTextarea.value = fhirJson;
                        showStatusMessage('Conversion successful, but error formatting JSON', true);
                    }
                    
                    // Make sure we're in view mode
                    fhirJsonTextarea.style.display = 'none';
                    fhirJsonViewer.style.display = 'block';
                    document.getElementById('editFhir').textContent = 'Edit';
                })
                .catch(error => {
                    fhirJsonViewer.innerHTML = '<div style="color: red; padding: 20px;">Conversion failed</div>';
                    showStatusMessage('Error converting JSON: ' + error.message, false);
                });
        } catch (e) {
            showStatusMessage('Error converting JSON: ' + e.message, false);
        }
    });
    
    // Load sample data on page load
    fhirJsonTextarea.value = formatJson(sampleFhirJson);
    availityJsonViewer.innerHTML = `<pre>${formatJson(sampleAvailityJson)}</pre>`;
    availityJsonEditor.value = formatJson(sampleAvailityJson);
    fhirJsonTextarea.value = formatJson(sampleFhirJson); 
    fhirJsonViewer.innerHTML = `<pre>${formatJson(sampleFhirJson)}</pre>`;
    fhirJsonEditor.value = formatJson(sampleFhirJson);  
});
