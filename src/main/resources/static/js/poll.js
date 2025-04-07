document.addEventListener('DOMContentLoaded', function () {
    const submitButton = document.getElementById('submitAndPoll');
    const stopButton = document.getElementById('stopPolling');
    const loadingSpan = document.getElementById('loading');
    const statusCard = document.getElementById('status-card');
    const pollingCard = document.getElementById('polling-card');
    const jsonCard = document.getElementById('json-card');   
    
    
    const resourceIdInput = document.getElementById('resourceId');
    const pollIntervalInput = document.getElementById('pollInterval');
    const maxAttemptsInput = document.getElementById('maxAttempts');

    submitButton.addEventListener('click', function () {
        // Get input values
        const resourceId = document.getElementById('resourceId').value.trim();
        const pollInterval = parseInt(document.getElementById('pollInterval').value) || 2;
        const maxAttempts = parseInt(document.getElementById('maxAttempts').value) || 10;

        // Validate inputs
        if (pollInterval < 1 || pollInterval > 10) {
            alert('Poll interval must be between 1 and 10 seconds');
            return;
        }

        if (maxAttempts < 1 || maxAttempts > 30) {
            alert('Maximum attempts must be between 1 and 30');
            return;
        }

        // Disable submit button, enable stop button
        submitButton.disabled = true;
        stopButton.disabled = false;

        // Show loading indicator
        loadingSpan.style.display = 'inline';

        // Clear previous results
        statusCard.style.display = 'none';
        pollingCard.style.display = 'none';
        jsonCard.style.display = 'none';

        // Submit new request or use existing ID
        if (resourceId) {
            // Use existing resource ID
            statusCard.style.display = 'block';
            statusCard.className = 'card success';
            document.getElementById('status-content').innerHTML = `
                    <p>Using existing resource ID for polling.</p>
                    <p><strong>Resource ID:</strong> ${resourceId}</p>
                `;

            // Start polling with existing ID
            startPolling(resourceId);
        }
        else {
            // Submit new request
            fetch('/prior-auth/submit', {
                method: 'POST'
            })
                .then(response => response.text())
                .then(data => {
                    // Show status card
                    statusCard.style.display = 'block';

                    if (data.includes('Successfully')) {
                        // Extract resource ID from response
                        const match = data.match(/Resource ID: ([^\n]+)/);
                        if (match && match[1]) {
                            const newId = match[1].trim();
                            resourceIdInput.value = newId;

                            // Update status card
                            statusCard.className = 'card success';
                            document.getElementById('status-content').innerHTML = `
                                <p>Successfully submitted prior authorization request to Availity.</p>
                                <p><strong>Resource ID:</strong> ${newId}</p>
                            `;

                            // Start polling with new ID
                            startPolling(newId);
                        } else {
                            // Could not extract resource ID
                            statusCard.className = 'card error';
                            document.getElementById('status-content').innerHTML = `
                                <p>Could not extract resource ID from response:</p>
                                <div class="info">${data}</div>
                            `;

                            // Enable submit button, disable stop button
                            document.getElementById('submitAndPoll').disabled = false;
                            document.getElementById('stopPolling').disabled = true;

                            // Hide loading indicator
                            loadingSpan.style.display = 'none';
                        }
                    } else {
                        // Submission failed
                        statusCard.className = 'card error';
                        document.getElementById('status-content').innerHTML = `
                            <p>Failed to submit prior authorization request:</p>
                            <div class="info">${data}</div>
                        `;

                        // Enable submit button, disable stop button
                        document.getElementById('submitAndPoll').disabled = false;
                        document.getElementById('stopPolling').disabled = true;

                        // Hide loading indicator
                        loadingSpan.style.display = 'none';
                    }
                })
                .catch(error => {
                    // Show error in status card
                    statusCard.style.display = 'block';
                    statusCard.className = 'card error';
                    document.getElementById('status-content').innerHTML = `
                        <p>Error submitting prior authorization request:</p>
                        <div class="info">${error.message}</div>
                    `;

                    // Enable submit button, disable stop button
                    document.getElementById('submitAndPoll').disabled = false;
                    document.getElementById('stopPolling').disabled = true;

                    // Hide loading indicator
                    loadingSpan.style.display = 'none';
                });
        }


    });

    function startPolling1(id, interval, maxAttempts) {
        // Show polling card
        pollingCard.style.display = 'block';
        pollingCard.className = 'card pending';

        // Show JSON card
        jsonCard.style.display = 'block';

        // Reset polling UI
        document.getElementById('max-attempts').textContent = maxAttempts;
        document.getElementById('attempt-count').textContent = '0';
        document.getElementById('elapsed-time').textContent = '0s';
        document.getElementById('status-text').textContent = 'Pending';
        document.getElementById('progress-bar').style.width = '0%';
        document.getElementById('json-content').textContent = 'No data available yet';

        // Start timer
        const startTime = new Date();
        const timerInterval = setInterval(function () {
            const elapsedSeconds = Math.floor((new Date() - startTime) / 1000);
            document.getElementById('elapsed-time').textContent = elapsedSeconds + 's';
        }, 1000);

        let attempts = 0;
        let pollTimer;
        let continuePolling = true;

        // Function to update progress
        function updateProgress(attempt, status) {
            const progressPercent = (attempt / maxAttempts) * 100;
            document.getElementById('progress-bar').style.width = progressPercent + '%';
            document.getElementById('attempt-count').textContent = attempt;
            document.getElementById('status-text').textContent = status;
        }

        // Function to handle polling completion
        function completePolling(success, status) {
            clearInterval(timerInterval);

            if (success) {
                pollingCard.className = 'card success';
            } else {
                pollingCard.className = 'card error';
            }

            updateProgress(attempts, status);

            // Enable submit button, disable stop button
            submitButton.disabled = false;
            stopButton.disabled = true;

            // Hide loading indicator
            loadingSpan.style.display = 'none';
        }

        // Stop polling button handler
        stopButton.addEventListener('click', function () {
            continuePolling = false;
            this.disabled = true;
            completePolling(false, "Stopped");
        });

        // Function to poll for status
        function poll() {
            if (!continuePolling || attempts >= maxAttempts) {
                if (attempts >= maxAttempts) {
                    completePolling(false, "Timeout");
                }
                return;
            }

            attempts++;
            updateProgress(attempts, 'Polling...');

            fetch(`/prior-auth/poll?id=${id}`)
                .then(response => response.text())
                .then(data => {
                    // Update JSON content
                    try {
                        const jsonContent = document.getElementById('json-content');
                        if (jsonContent) {
                            // Try to extract JSON from the response
                            let jsonData = data;

                            // Look for JSON-like content in the response
                            const jsonMatch = data.match(/\\{[\\s\\S]*\\}/);
                            if (jsonMatch) {
                                jsonData = jsonMatch[0];

                                // Try to parse and format the JSON
                                try {
                                    const formattedJson = JSON.stringify(JSON.parse(jsonData), null, 2);
                                    jsonContent.textContent = formattedJson;
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

                    // Determine status
                    let simpleStatus = 'Pending';
                    if (data.includes('Approved')) {
                        simpleStatus = 'Approved';
                    } else if (data.includes('Denied')) {
                        simpleStatus = 'Denied';
                    } else if (data.includes('Pended')) {
                        simpleStatus = 'Pended';
                    } else if (data.includes('Complete')) {
                        simpleStatus = 'Complete';
                    } else if (data.includes('Error')) {
                        simpleStatus = 'Error';
                    }

                    // Update progress
                    updateProgress(attempts, simpleStatus);

                    // Check if we should stop polling
                    if (simpleStatus === 'Approved' || simpleStatus === 'Denied' || simpleStatus === 'Pended' || simpleStatus === 'Complete') {
                        // Polling complete with success
                        completePolling(true, simpleStatus);
                    } else if (attempts >= maxAttempts) {
                        // Reached max attempts
                        completePolling(false, 'Max attempts reached');
                    } else if (continuePolling) {
                        // Continue polling after interval
                        pollTimer = setTimeout(poll, interval * 1000);
                    }
                })
                .catch(error => {
                    console.error('Polling error:', error);
                    updateProgress(attempts, 'Error');

                    // Update JSON content with error
                    const jsonContent = document.getElementById('json-content');
                    if (jsonContent) {
                        jsonContent.textContent = 'Error: ' + error.message;
                    }

                    // Check if we should stop polling
                    if (attempts >= maxAttempts) {
                        // Reached max attempts
                        completePolling(false, 'Max attempts reached');
                    } else if (continuePolling) {
                        // Continue polling after interval
                        pollTimer = setTimeout(poll, interval * 1000);
                    }
                });
        }

        // Start polling
        poll();
    }

    // Function to start polling
    function startPolling(id) {
        // Show polling and JSON cards
        pollingCard.style.display = 'block';
        pollingCard.className = 'card pending';
        jsonCard.style.display = 'block';

        // Reset polling UI
        document.getElementById('attempt-count').textContent = '0';
        document.getElementById('elapsed-time').textContent = '0s';
        document.getElementById('status-text').textContent = 'Pending';
        document.getElementById('progress-bar').style.width = '0%';
        document.getElementById('json-content').textContent = 'No data available yet';

        // Start timer
        const startTime = new Date();
        const timerInterval = setInterval(function () {
            const elapsedSeconds = Math.floor((new Date() - startTime) / 1000);
            document.getElementById('elapsed-time').textContent = elapsedSeconds + 's';
        }, 1000);

        let attempts = 0;
        let pollTimer;
        let continuePolling = true;

        // Function to update progress
        function updateProgress(attempt, status) {
            const progressPercent = (attempt / maxAttempts) * 100;
            document.getElementById('progress-bar').style.width = progressPercent + '%';
            document.getElementById('attempt-count').textContent = attempt;
            document.getElementById('status-text').textContent = status;
        }

        // Function to handle polling completion
        function completePolling(success, status) {
            clearInterval(timerInterval);

            if (success) {
                pollingCard.className = 'card success';
            } else {
                pollingCard.className = 'card error';
            }

            updateProgress(attempts, status);

            // Enable submit button, disable stop button
            document.getElementById('submitAndPoll').disabled = false;
            document.getElementById('stopPolling').disabled = true;

            // Hide loading indicator
            loadingSpan.style.display = 'none';
        }

        // Stop polling button handler
        document.getElementById('stopPolling').addEventListener('click', function () {
            continuePolling = false;
            this.disabled = true;
            completePolling(false, "Stopped");
        });

        // Function to poll for status
        function poll() {
            if (!continuePolling || attempts >= maxAttempts) {
                if (attempts >= maxAttempts) {
                    completePolling(false, "Timeout");
                }
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

                            // Look for JSON-like content in the response
                            const jsonMatch = data.match(/\{[\s\S]*\}/);
                            if (jsonMatch) {
                                jsonData = jsonMatch[0];

                                // Try to parse and format the JSON
                                try {
                                    const formattedJson = JSON.stringify(JSON.parse(jsonData), null, 2);
                                    jsonContent.textContent = formattedJson;
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
                        completePolling(true, simpleStatus);
                    } else if (continuePolling && attempts < maxAttempts) {
                        // Continue polling
                        pollTimer = setTimeout(poll, pollInterval * 1000);
                    } else {
                        completePolling(false, "Timeout");
                    }
                })
                .catch(error => {
                    console.error('Error polling for status:', error);
                    updateProgress(attempts, 'Error');

                    // Continue polling despite error
                    if (continuePolling && attempts < maxAttempts) {
                        pollTimer = setTimeout(poll, pollInterval * 1000);
                    } else {
                        completePolling(false, "Error: " + error.message);
                    }
                });
        }

        // Check if the prior auth is already completed
        fetch(`/prior-auth/polling/${id}`)
            .then(response => response.json())
            .then(data => {
                console.log('DEBUGGING: Checking if prior auth is already completed:', data);

                if (data.completed === true) {
                    console.log('DEBUGGING: Prior auth is already completed, not starting polling');
                    updateProgress(attempts, data.statusDescription || 'Completed');
                    completePolling(true, data.statusDescription || 'Completed');
                } else {
                    // Start polling
                    poll();
                }
            })
            .catch(error => {
                console.error('Error checking prior auth status:', error);
                // Start polling anyway
                poll();
            });
    }


});
