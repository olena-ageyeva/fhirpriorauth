// Sample FHIR request
let sampleFhirRequest = null;

async function loadMockFhirRequest() {
    try {
        const response = await fetch('/mock-data/mock-fhir-claim.json');
        sampleFhirRequest = await response.json();
        populateRequestDetails(); // now safe to populate after load
    } catch (error) {
        console.error('Failed to load mock FHIR request:', error);
    }
}

// Function to convert FHIR to Availity using the server-side mapper
async function convertFhirToAvailityRequest(fhirJson) {
    try {
        // Parse FHIR JSON if it's a string
        const fhirObj = typeof fhirJson === 'string' ? JSON.parse(fhirJson) : fhirJson;

        // Call the server-side mapper endpoint
        const response = await fetch('/api/mapper/fhir-to-availity', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(fhirObj)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Server returned ${response.status}: ${errorText}`);
        }

        // Parse the response
        const availityJson = await response.json();
        return availityJson;
    } catch (e) {
        console.error('Error converting FHIR to Availity:', e);

        // Fallback to client-side conversion if server fails
        // console.log('Falling back to client-side conversion');

        // // Parse FHIR JSON if it's a string
        // const fhirObj = typeof fhirJson === 'string' ? JSON.parse(fhirJson) : fhirJson;

        // // Create a simple Availity service review object
        // const serviceReview = {
        //     // Payer information
        //     payer: {
        //         id: fhirObj.insurer?.identifier?.value || 'BCBSF',
        //         name: fhirObj.insurer?.display || 'FLORIDA BLUE'
        //     },

        //     // Provider information
        //     requestingProvider: {
        //         npi: fhirObj.provider?.identifier?.value || '1234567893',
        //         lastName: fhirObj.provider?.display?.split(' ')[1] || 'PROVIDER',
        //         firstName: fhirObj.provider?.display?.split(' ')[0] || 'TEST',
        //         roleCode: '1P',
        //         addressLine1: '123 Provider Street',
        //         city: 'JACKSONVILLE',
        //         stateCode: 'FL',
        //         zipCode: '32223',
        //         phone: '9043334444',
        //         contactName: 'John Doe'
        //     },

        //     // Subscriber information
        //     subscriber: {
        //         memberId: fhirObj.patient?.identifier?.value || 'ASBA1274712',
        //         firstName: fhirObj.patient?.display?.split(' ')[0] || 'TEST',
        //         lastName: fhirObj.patient?.display?.split(' ')[1] || 'PATIENT'
        //     },

        //     // Patient information
        //     patient: {
        //         firstName: 'TEST',
        //         lastName: 'PATIENTONE',
        //         subscriberRelationshipCode: '18',
        //         birthDate: '1990-01-01'
        //     },

        //     // Diagnoses
        //     diagnoses: fhirObj.diagnosis?.map(diag => ({
        //         qualifierCode: 'ABK',
        //         code: diag.diagnosisCodeableConcept?.coding?.[0]?.code || '78900'
        //     })) || [{
        //         qualifierCode: 'ABK',
        //         code: '78900'
        //     }],

        //     // Request metadata
        //     requestTypeCode: 'HS',
        //     serviceTypeCode: '73',
        //     placeOfServiceCode: '22',
        //     serviceLevelCode: 'E',
        //     fromDate: '2022-09-02',
        //     toDate: '2022-09-13',
        //     quantity: '1',
        //     quantityTypeCode: 'VS',

        //     // Procedures
        //     procedures: fhirObj.procedure?.map(proc => ({
        //         fromDate: proc.date || '2022-09-02',
        //         toDate: proc.date || '2022-09-13',
        //         code: proc.procedureCodeableConcept?.coding?.[0]?.code || '99213',
        //         qualifierCode: 'HC',
        //         quantity: '1',
        //         quantityTypeCode: 'UN'
        //     })) || [{
        //         fromDate: '2022-09-02',
        //         toDate: '2022-09-13',
        //         code: '99213',
        //         qualifierCode: 'HC',
        //         quantity: '1',
        //         quantityTypeCode: 'UN'
        //     }],

        //     // Rendering Providers
        //     renderingProviders: [{
        //         lastName: 'PROVIDERONE',
        //         firstName: 'TEST',
        //         npi: '1234567891',
        //         taxId: '111111111',
        //         roleCode: '71',
        //         addressLine1: '111 HEALTHY PKWY',
        //         city: 'JACKSONVILLE',
        //         stateCode: 'FL',
        //         zipCode: '22222'
        //     }]
        // };

        // return { serviceReview };
    }
}

// Function to convert Availity to FHIR using the server-side mapper
async function convertToFhir(availityJson) {
    try {
        // Parse Availity JSON if it's a string
        const availityObj = typeof availityJson === 'string' ? JSON.parse(availityJson) : availityJson;

        // Call the server-side mapper endpoint
        const response = await fetch('/api/mapper/availity-to-fhir', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(availityObj)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Server returned ${response.status}: ${errorText}`);
        }

        // Parse the response
        const fhirClaim = await response.json();
        return fhirClaim;
    } catch (e) {
        console.error('Error converting Availity to FHIR:', e);

        // Fallback to client-side conversion if server fails
        // console.log('Falling back to client-side conversion');

        // // Parse Availity JSON if it's a string
        // const availityObj = typeof availityJson === 'string' ? JSON.parse(availityJson) : availityJson;

        // // Extract serviceReview if it exists
        // const serviceReview = availityObj.serviceReview || availityObj;

        // // Create a FHIR Claim resource
        // const fhirClaim = {
        //     resourceType: 'Claim',
        //     id: 'example-claim-' + Math.floor(Math.random() * 10000),
        //     status: 'active',
        //     use: 'preauthorization',
        //     created: new Date().toISOString(),

        //     // Patient information
        //     patient: {
        //         reference: 'Patient/' + (serviceReview.patient?.lastName || 'PATIENT'),
        //         display: (serviceReview.patient?.firstName || '') + ' ' + (serviceReview.patient?.lastName || 'PATIENT')
        //     },

        //     // Provider information
        //     provider: {
        //         reference: 'Organization/' + (serviceReview.requestingProvider?.npi || '1234567893'),
        //         display: serviceReview.requestingProvider?.lastName || 'PROVIDER'
        //     },

        //     // Insurer information
        //     insurer: {
        //         reference: 'Organization/' + (serviceReview.payer?.id || 'BCBSF'),
        //         display: serviceReview.payer?.name || 'FLORIDA BLUE'
        //     },

        //     // Diagnoses
        //     diagnosis: serviceReview.diagnoses?.map((diag, index) => ({
        //         sequence: index + 1,
        //         diagnosisCodeableConcept: {
        //             coding: [{
        //                 system: 'http://hl7.org/fhir/sid/icd-10',
        //                 code: diag.code || '78900',
        //                 display: 'Diagnosis ' + (index + 1)
        //             }]
        //         }
        //     })) || [],

        //     // Procedures
        //     procedure: serviceReview.procedures?.map((proc, index) => ({
        //         sequence: index + 1,
        //         procedureCodeableConcept: {
        //             coding: [{
        //                 system: 'http://www.ama-assn.org/go/cpt',
        //                 code: proc.code || '99213',
        //                 display: 'Procedure ' + (index + 1)
        //             }]
        //         },
        //         date: proc.fromDate || '2022-09-02'
        //     })) || [],

        //     // Insurance
        //     insurance: [{
        //         sequence: 1,
        //         focal: true,
        //         coverage: {
        //             reference: 'Coverage/' + (serviceReview.subscriber?.memberId || 'ASBA1274712'),
        //             display: 'Coverage for ' + (serviceReview.subscriber?.lastName || 'PATIENT')
        //         }
        //     }],

        //     // Item
        //     item: serviceReview.procedures?.map((proc, index) => ({
        //         sequence: index + 1,
        //         productOrService: {
        //             coding: [{
        //                 system: 'http://www.ama-assn.org/go/cpt',
        //                 code: proc.code || '99213',
        //                 display: 'Service ' + (index + 1)
        //             }]
        //         },
        //         servicedPeriod: {
        //             start: proc.fromDate || '2022-09-02',
        //             end: proc.toDate || '2022-09-13'
        //         },
        //         quantity: {
        //             value: parseInt(proc.quantity || '1')
        //         }
        //     })) || []
        // };

        // return fhirClaim;
    }
}

// Function to load cached data
function loadCachedData() {
    try {
        const cachedData = localStorage.getItem('priorAuthResponse');
        if (cachedData) {
            const data = JSON.parse(cachedData);
            const resultDiv = document.getElementById('result');

            // Create status card
            const statusCard = document.createElement('div');
            statusCard.className = 'card success';
            statusCard.innerHTML = `
                <h2>Cached Submission</h2>
                <p>Showing cached prior authorization request. Submit a new request to update.</p>
                <p><strong>Resource ID:</strong> ${data.resourceId}</p>
            `;
            resultDiv.appendChild(statusCard);

            // Create polling card if it exists in cache
            if (data.pollingStatus) {
                const pollingCard = document.createElement('div');
                pollingCard.className = 'card ' + (data.pollingStatus.success ? 'success' : 'pending');
                pollingCard.id = 'polling-card';
                pollingCard.innerHTML = `
                    <h2>Polling Status</h2>
                    <div class="poll-info">
                        <div>Attempt: <span id="attempt-count">${data.pollingStatus.attempts}</span>/<span id="max-attempts">10</span></div>
                         <div>Elapsed: <span id="elapsed-time">${data.pollingStatus.time}</span></div>
                        <div>Status: <span id="status-text">${data.pollingStatus.status}</span></div>
                    </div>
                    <div class="progress-container">
                        <div class="progress-bar" id="progress-bar" style="width: ${(data.pollingStatus.attempts / 10) * 100}%"></div>
                    </div>
                `;
                resultDiv.appendChild(pollingCard);
            }

            // Create JSON card with tabs
            const jsonCard = document.createElement('div');
            jsonCard.className = 'card';
            jsonCard.id = 'json-card';
            jsonCard.innerHTML = `
                <h2>Response Details</h2>
                <div class="tabs">
                    <button class="tab-button active" data-tab="availity">Availity (Original)</button>
                    <button class="tab-button" data-tab="fhir">FHIR (Converted)</button>
                </div>
                <div class="tab-content">
                    <div id="availity-tab" class="tab-pane active">
                        <details id="json-viewer" open>
                            <summary>Availity JSON Response</summary>
                            <pre id="json-content">${data.availityJson || 'No data available'}</pre>
                        </details>
                    </div>
                    <div id="fhir-tab" class="tab-pane">
                        <details id="fhir-viewer" open>
                            <summary>Converted FHIR Resource</summary>
                            <pre id="fhir-content">${data.fhirJson || 'No data available yet'}</pre>
                        </details>
                    </div>
                </div>
            `;
            resultDiv.appendChild(jsonCard);

            // Add event listeners for tab buttons
            document.querySelectorAll('.tab-button').forEach(button => {
                button.addEventListener('click', function () {
                    const tabId = this.getAttribute('data-tab');
                    switchTab(tabId);
                });
            });
        }
    } catch (e) {
        console.error('Error loading cached data:', e);
        // Clear the cache if there's an error
        localStorage.removeItem('priorAuthResponse');
    }
}

// Function to save response data to cache
function saveCachedData(resourceId, availityJson, fhirJson, pollingStatus) {
    try {
        const cacheData = {
            resourceId: resourceId,
            availityJson: availityJson,
            fhirJson: fhirJson,
            pollingStatus: pollingStatus,
            timestamp: new Date().toISOString()
        };
        localStorage.setItem('priorAuthResponse', JSON.stringify(cacheData));
    } catch (e) {
        console.error('Error saving cached data:', e);
    }
}

// Function to clear cached data
function clearCachedData() {
    localStorage.removeItem('priorAuthResponse');
}

// Function to switch tabs
function switchTab(tabId) {
    // Determine which card contains the tab
    let cardSelector = '#json-card';
    if (tabId.includes('request')) {
        // This is a request tab, so we don't need to do anything special
        return;
    }

    // Hide all tab panes in the response card
    const tabPanes = document.querySelectorAll(`${cardSelector} .tab-pane`);
    tabPanes.forEach(pane => pane.classList.remove('active'));

    // Deactivate all tab buttons in the response card
    const tabButtons = document.querySelectorAll(`${cardSelector} .tab-button`);
    tabButtons.forEach(button => button.classList.remove('active'));

    // Activate the selected tab
    document.getElementById(tabId + '-tab').classList.add('active');
    document.querySelector(`${cardSelector} .tab-button[data-tab="${tabId}"]`).classList.add('active');

    // If switching to FHIR tab, automatically convert the Availity response
    if (tabId === 'fhir') {
        const jsonContent = document.getElementById('json-content');
        const fhirContent = document.getElementById('fhir-content');

        if (jsonContent && fhirContent && jsonContent.textContent && jsonContent.textContent !== 'No data available' && jsonContent.textContent !== 'No data available yet') {
            // Show loading message
            fhirContent.textContent = 'Converting...';

            try {
                // Parse the Availity JSON
                const availityJson = JSON.parse(jsonContent.textContent);

                // Convert to FHIR (async function)
                convertToFhir(availityJson)
                    .then(fhirClaim => {
                        // Format and display the FHIR claim
                        const formattedJson = JSON.stringify(fhirClaim, null, 2);
                        fhirContent.textContent = formattedJson;

                        // Save the FHIR JSON to cache
                        const availityJsonText = jsonContent.textContent;
                        const resourceId = availityJson?.id;
                        if (resourceId) {
                            // Get the current polling status from the UI
                            const attemptCount = document.getElementById('attempt-count')?.textContent || '0';
                            const statusText = document.getElementById('status-text')?.textContent || 'Unknown';
                            const pollingStatus = {
                                attempts: parseInt(attemptCount) || 0,
                                time: 0,
                                status: statusText,
                                success: statusText === 'Complete' || statusText === 'Approved' || statusText === 'Denied' || statusText === 'Pended'
                            };
                            saveCachedData(resourceId, availityJsonText, formattedJson, pollingStatus);
                        }
                    })
                    .catch(error => {
                        console.error('Error automatically converting to FHIR:', error);
                        fhirContent.textContent = 'Error converting to FHIR: ' + error.message;
                    });
            } catch (e) {
                console.error('Error parsing Availity JSON:', e);
                fhirContent.textContent = 'Error parsing Availity JSON: ' + e.message;
            }
        }
    }
}

// Function to switch request tabs
function switchRequestTab(tabId) {
    // Hide all request tab panes
    const tabPanes = document.querySelectorAll('#request-details-card .tab-pane');
    tabPanes.forEach(pane => pane.classList.remove('active'));

    // Deactivate all request tab buttons
    const tabButtons = document.querySelectorAll('#request-details-card .tab-button');
    tabButtons.forEach(button => button.classList.remove('active'));

    // Activate the selected tab
    document.getElementById(tabId + '-tab').classList.add('active');
    document.querySelector(`#request-details-card .tab-button[data-tab="${tabId}"]`).classList.add('active');
}

// Function to populate the Request Details section
async function populateRequestDetails() {
    if (!sampleFhirRequest) return;

    // Populate FHIR request
    const fhirRequestContent = document.getElementById('fhir-request-content');
    if (fhirRequestContent) {
        fhirRequestContent.textContent = JSON.stringify(sampleFhirRequest, null, 2);
    }

    // Convert FHIR to Availity and populate Availity request
    const availityRequestContent = document.getElementById('availity-request-content');
    if (availityRequestContent) {
        try {
            // Show loading message
            availityRequestContent.textContent = 'Converting...';

            // Convert FHIR to Availity using the server-side mapper
            const availityRequest = await convertFhirToAvailityRequest(sampleFhirRequest);
            availityRequestContent.textContent = JSON.stringify(availityRequest, null, 2);
        } catch (error) {
            console.error('Error populating Availity request:', error);
            availityRequestContent.textContent = 'Error converting FHIR to Availity: ' + error.message;
        }
    }
}

document.addEventListener('DOMContentLoaded', function () {
    // Load cached data
    loadCachedData();

    // Populate request details
    loadMockFhirRequest();

    // Add event listeners for request tab buttons
    document.querySelectorAll('.tab-button[data-tab^="fhir-request"], .tab-button[data-tab^="availity-request"]').forEach(button => {
        button.addEventListener('click', function () {
            const tabId = this.getAttribute('data-tab');
            switchRequestTab(tabId);
        });
    });

    document.getElementById('submitAuth').addEventListener('click', function () {
        const resultDiv = document.getElementById('result');
        const loadingSpan = document.getElementById('loading');

        // Show loading indicator
        loadingSpan.style.display = 'inline';

        // Clear previous results
        resultDiv.innerHTML = '';

        // Clear cached data when submitting a new request
        clearCachedData();

        // Submit the prior auth request
        fetch('/prior-auth/submit', { method: 'POST' })
            .then(response => response.text())
            .then(data => {
                // Hide loading indicator
                loadingSpan.style.display = 'none';

                // Create result card
                const card = document.createElement('div');
                card.className = 'card';

                if (data.includes('Resource ID:')) {
                    // Success
                    card.classList.add('success');

                    // Extract resource ID
                    const resourceIdMatch = data.match(/Resource ID: ([^\s]+)/);
                    const resourceId = resourceIdMatch ? resourceIdMatch[1] : 'Unknown';

                    card.innerHTML = `
                        <h2>Submission Successful</h2>
                        <p>Successfully submitted prior authorization request.</p>
                        <p><strong>Resource ID:</strong> ${resourceId}</p>
                    `;

                    // Create polling card
                    const pollingCard = document.createElement('div');
                    pollingCard.className = 'card pending';
                    pollingCard.id = 'polling-card';
                    pollingCard.innerHTML = `
                        <h2>Polling for Status Updates</h2>
                        <div class="poll-info">
                            <div>Attempt: <span id="attempt-count">0</span>/<span id="max-attempts">10</span></div>
                            <div>Elapsed: <span id="elapsed-time">0</span></div>
                            <div>Status: <span id="status-text">Pending</span></div>
                        </div>
                        <div class="progress-container">
                            <div class="progress-bar" id="progress-bar"></div>
                        </div>
                    `;

                    // Create JSON card with tabs
                    const jsonCard = document.createElement('div');
                    jsonCard.className = 'card';
                    jsonCard.id = 'json-card';
                    jsonCard.innerHTML = `
                        <h2>Response Details</h2>
                        <div class="tabs">
                            <button class="tab-button active" data-tab="availity">Availity (Original)</button>
                            <button class="tab-button" data-tab="fhir">FHIR (Converted)</button>
                        </div>
                        <div class="tab-content">
                            <div id="availity-tab" class="tab-pane active">
                                <details id="json-viewer" open>
                                    <summary>Availity JSON Response</summary>
                                    <pre id="json-content">No data available yet</pre>
                                </details>
                            </div>
                            <div id="fhir-tab" class="tab-pane">
                                <details id="fhir-viewer" open>
                                    <summary>Converted FHIR Resource</summary>
                                    <pre id="fhir-content">No data available yet</pre>
                                </details>
                            </div>
                        </div>
                    `;

                    // Add cards to result div
                    resultDiv.appendChild(card);
                    resultDiv.appendChild(pollingCard);
                    resultDiv.appendChild(jsonCard);

                    // Add event listeners for tab buttons
                    document.querySelectorAll('.tab-button').forEach(button => {
                        button.addEventListener('click', function () {
                            const tabId = this.getAttribute('data-tab');
                            switchTab(tabId);
                        });
                    });

                    // Save initial response data to cache
                    saveCachedData(resourceId.trim(), null, null, null);

                    // Start polling
                    startPolling(resourceId.trim());
                } else {
                    // Error
                    card.classList.add('error');
                    card.innerHTML = `
                        <h2>Submission Failed</h2>
                        <p>Error - ${data}</p>
                        <p>Please check the logs for more details.</p>
                    `;
                    resultDiv.appendChild(card);
                }
            })
            .catch(error => {
                // Hide loading indicator
                loadingSpan.style.display = 'none';

                // Create error card
                const card = document.createElement('div');
                card.className = 'card error';
                card.innerHTML = `
                    <h2>Submission Failed</h2>
                    <p>Error - ${error.message}</p>
                    <p>Please check the logs for more details.</p>
                `;
                resultDiv.appendChild(card);
            });
    });

    function startPolling(id) {
        console.log('Starting polling for ID:', id);
        const maxAttempts = 10;
        const pollInterval = 2000; // 2 seconds
        let attempts = 0;
        let pollTimer;
        let elapsedSeconds = 0;

        // Start timer
        const startTime = new Date();
        const timerInterval = setInterval(function () {
            elapsedSeconds = Math.floor((new Date() - startTime) / 1000);
            const elapsedTimeElement = document.getElementById('elapsed-time');
            if (elapsedTimeElement) {
                elapsedTimeElement.textContent = elapsedSeconds + 's';
            }
        }, 1000);

        // Function to update progress
        function updateProgress(attempt, status) {
            const progressPercent = (attempt / maxAttempts) * 100;

            const progressBar = document.getElementById('progress-bar');
            if (progressBar) {
                progressBar.style.width = progressPercent + '%';
            }

            const attemptCount = document.getElementById('attempt-count');
            if (attemptCount) {
                attemptCount.textContent = attempt;
            }

            const statusText = document.getElementById('status-text');
            if (statusText) {
                statusText.textContent = status;
            }
        }

        // Function to handle polling completion
        function completePolling(success, status) {
            clearInterval(timerInterval);

            const pollingCard = document.getElementById('polling-card');
            if (pollingCard) {
                if (success) {
                    pollingCard.className = 'card success';
                } else {
                    pollingCard.className = 'card error';
                }
            }

            updateProgress(attempts, status);
        }

        // Function to poll for status
        function poll() {
            if (attempts >= maxAttempts) {
                clearTimeout(pollTimer);
                completePolling(false, "Timeout");
                return;
            }

            attempts++;            
            updateProgress(attempts, 'Polling...');

            fetch(`/prior-auth/submit/${id}/status`)
                .then(response => response.text())
                .then(data => {
                    // Extract status from response
                    let status = 'Unknown';
                    if (data.includes('Status:')) {
                        status = data.split('Status:')[1].split('\n')[0].trim();
                    }

                    // Simplify status for display
                    let simpleStatus = 'Pending';
                    if (status.includes('Complete')) simpleStatus = 'Complete';
                    else if (status.includes('Approved')) simpleStatus = 'Approved';
                    else if (status.includes('Denied')) simpleStatus = 'Denied';
                    else if (status.includes('Pended')) simpleStatus = 'Pended';
                    else if (status.includes('Error')) simpleStatus = 'Error';

                    updateProgress(attempts, simpleStatus);

                    // Update JSON viewer
                    try {
                        const jsonContent = document.getElementById('json-content');
                        if (jsonContent) {
                            // Try to extract JSON from the response
                            let jsonData = data;
                            let parsedJson = null;

                            // Look for JSON-like content in the response
                            const jsonMatch = data.match(/\{[\s\S]*\}/);
                            if (jsonMatch) {
                                jsonData = jsonMatch[0];

                                // Try to parse and format the JSON
                                try {
                                    parsedJson = JSON.parse(jsonData);
                                    const formattedJson = JSON.stringify(parsedJson, null, 2);
                                    jsonContent.textContent = formattedJson;

                                    // Save the Availity JSON to cache
                                    const pollingStatus = {
                                        attempts: attempts,
                                        time: elapsedSeconds || 0,
                                        status: simpleStatus,
                                        success: simpleStatus === 'Complete' || simpleStatus === 'Approved' || simpleStatus === 'Denied' || simpleStatus === 'Pended'
                                    };
                                    saveCachedData(id, formattedJson, document.getElementById('fhir-content')?.textContent, pollingStatus);

                                    // Automatically convert to FHIR
                                    try {
                                        // Only convert if we're in the FHIR tab or if the FHIR content is empty
                                        const fhirTab = document.getElementById('fhir-tab');
                                        const fhirContent = document.getElementById('fhir-content');

                                        if ((fhirTab && fhirTab.classList.contains('active')) ||
                                            (fhirContent && (!fhirContent.textContent ||
                                                fhirContent.textContent === 'No data available yet' ||
                                                fhirContent.textContent === 'Converting...'))) {
                                            convertToFhir(parsedJson);
                                        }
                                    } catch (e) {
                                        console.error('Error automatically converting to FHIR during polling:', e);
                                    }
                                } catch (e) {
                                    // If JSON parsing fails, just show the raw response
                                    jsonContent.textContent = jsonData;
                                }
                            } else {
                                jsonContent.textContent = data;
                            }
                        }
                    } catch (e) {
                        console.error('Error updating JSON viewer:', e);
                    }

                    // Check if complete
                    if (data.includes('Complete') || data.includes('Approved') || data.includes('Denied') || data.includes('Pended')) {
                        clearTimeout(pollTimer);
                        completePolling(true, simpleStatus);
                    } else {
                        // Continue polling
                        pollTimer = setTimeout(poll, pollInterval);
                    }
                })
                .catch(error => {
                    console.error('Error polling for status:', error);
                    updateProgress(attempts, 'Error');

                    // Continue polling despite error
                    pollTimer = setTimeout(poll, pollInterval);
                });
        }

        // Start polling
        poll();
    }


});
