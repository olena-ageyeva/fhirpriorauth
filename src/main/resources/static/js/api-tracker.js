// API Tracker JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // DOM elements
    const refreshBtn = document.getElementById('refresh-btn');
    const clearLogsBtn = document.getElementById('clear-logs-btn');
    const endpointFilter = document.getElementById('endpoint-filter');
    const dateFilter = document.getElementById('date-filter');
    
    const submitCallsTable = document.getElementById('submit-calls-table');
    const statusCallsTable = document.getElementById('status-calls-table');
    const submitCallsBody = document.getElementById('submit-calls-body');
    const statusCallsBody = document.getElementById('status-calls-body');
    
    const submitPagination = document.getElementById('submit-pagination');
    const statusPagination = document.getElementById('status-pagination');
    
    const requestModal = document.getElementById('request-modal');
    const closeModalBtn = document.querySelector('.close');
    
    // Tab handling
    const tabButtons = document.querySelectorAll('.tab-button');
    tabButtons.forEach(button => {
        button.addEventListener('click', function() {
            const tabId = this.getAttribute('data-tab');
            switchTab(tabId);
        });
    });
    
    // Pagination state
    const paginationState = {
        submit: {
            currentPage: 1,
            totalPages: 1,
            pageSize: 10
        },
        status: {
            currentPage: 1,
            totalPages: 1,
            pageSize: 10
        }
    };
    
    // Load initial data
    loadApiCalls();
    
    // Event listeners
    refreshBtn.addEventListener('click', loadApiCalls);
    
    clearLogsBtn.addEventListener('click', function() {
        if (confirm('Are you sure you want to clear all API call logs? This action cannot be undone.')) {
            clearApiLogs();
        }
    });
    
    endpointFilter.addEventListener('change', loadApiCalls);
    dateFilter.addEventListener('change', loadApiCalls);
    
    closeModalBtn.addEventListener('click', function() {
        requestModal.style.display = 'none';
    });
    
    window.addEventListener('click', function(event) {
        if (event.target === requestModal) {
            requestModal.style.display = 'none';
        }
    });
    
    // Functions
    function loadApiCalls() {
        const endpoint = endpointFilter.value;
        const dateRange = dateFilter.value;
        
        // Show loading state
        submitCallsBody.innerHTML = '<tr><td colspan="5">Loading...</td></tr>';
        statusCallsBody.innerHTML = '<tr><td colspan="5">Loading...</td></tr>';
        
        // Fetch submit calls
        fetchApiCalls('/api/tracker/calls', {
            endpoint: endpoint === 'all' ? '/submit' : endpoint,
            dateRange: dateRange,
            page: paginationState.submit.currentPage,
            pageSize: paginationState.submit.pageSize
        })
        .then(data => {
            renderApiCalls(data, 'submit');
        })
        .catch(error => {
            submitCallsBody.innerHTML = `<tr><td colspan="5">Error loading data: ${error.message}</td></tr>`;
        });
        
        // Fetch status calls
        fetchApiCalls('/api/tracker/calls', {
            endpoint: endpoint === 'all' ? '/status' : endpoint,
            dateRange: dateRange,
            page: paginationState.status.currentPage,
            pageSize: paginationState.status.pageSize
        })
        .then(data => {
            renderApiCalls(data, 'status');
        })
        .catch(error => {
            statusCallsBody.innerHTML = `<tr><td colspan="5">Error loading data: ${error.message}</td></tr>`;
        });
    }
    
    function fetchApiCalls(url, params) {
        // Build query string
        const queryString = Object.keys(params)
            .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
            .join('&');
        
        return fetch(`${url}?${queryString}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error ${response.status}`);
                }
                return response.json();
            });
    }
    
    function renderApiCalls(data, type) {
        const tableBody = type === 'submit' ? submitCallsBody : statusCallsBody;
        const pagination = type === 'submit' ? submitPagination : statusPagination;
        
        // Update pagination state
        paginationState[type].totalPages = data.totalPages || 1;
        
        // Clear existing rows
        tableBody.innerHTML = '';
        
        if (data.calls && data.calls.length > 0) {
            // Render table rows
            data.calls.forEach(call => {
                const row = document.createElement('tr');
                
                // Format timestamp
                const timestamp = new Date(call.timestamp).toLocaleString();
                
                // Create table cells
                row.innerHTML = `
                    <td>${timestamp}</td>
                    <td>${call.clientIp}</td>
                    <td>${call.requestId}</td>
                    <td class="status-${call.status.toLowerCase()}">${call.status}</td>
                    <td>
                        <button class="action-btn view-btn" data-id="${call.id}">View</button>
                    </td>
                `;
                
                tableBody.appendChild(row);
            });
            
            // Add event listeners to view buttons
            tableBody.querySelectorAll('.view-btn').forEach(button => {
                button.addEventListener('click', function() {
                    const callId = this.getAttribute('data-id');
                    viewCallDetails(callId);
                });
            });
        } else {
            // No data
            tableBody.innerHTML = '<tr><td colspan="5">No API calls found</td></tr>';
        }
        
        // Render pagination
        renderPagination(pagination, type);
    }
    
    function renderPagination(container, type) {
        container.innerHTML = '';
        
        const { currentPage, totalPages } = paginationState[type];
        
        // Previous button
        const prevButton = document.createElement('button');
        prevButton.textContent = '← Previous';
        prevButton.disabled = currentPage === 1;
        prevButton.addEventListener('click', () => {
            if (currentPage > 1) {
                paginationState[type].currentPage--;
                loadApiCalls();
            }
        });
        container.appendChild(prevButton);
        
        // Page numbers
        const startPage = Math.max(1, currentPage - 2);
        const endPage = Math.min(totalPages, startPage + 4);
        
        for (let i = startPage; i <= endPage; i++) {
            const pageButton = document.createElement('button');
            pageButton.textContent = i;
            pageButton.classList.toggle('active', i === currentPage);
            pageButton.addEventListener('click', () => {
                paginationState[type].currentPage = i;
                loadApiCalls();
            });
            container.appendChild(pageButton);
        }
        
        // Next button
        const nextButton = document.createElement('button');
        nextButton.textContent = 'Next →';
        nextButton.disabled = currentPage === totalPages;
        nextButton.addEventListener('click', () => {
            if (currentPage < totalPages) {
                paginationState[type].currentPage++;
                loadApiCalls();
            }
        });
        container.appendChild(nextButton);
    }
    
    function viewCallDetails(callId) {
        // Fetch call details
        fetch(`/api/tracker/calls/${callId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                // Populate modal with call details
                const fhirContent = document.getElementById('request-fhir-content');
                const availityContent = document.getElementById('request-availity-content');
                const responseContent = document.getElementById('response-content');
                
                // Format JSON for display
                fhirContent.textContent = formatJson(data.fhirPayload);
                availityContent.textContent = formatJson(data.availityPayload);
                responseContent.textContent = formatJson(data.response);
                
                // Show modal
                requestModal.style.display = 'block';
                
                // Activate FHIR tab by default
                switchModalTab('request-fhir');
            })
            .catch(error => {
                alert(`Error loading call details: ${error.message}`);
            });
    }
    
    function clearApiLogs() {
        fetch('/api/tracker/calls', {
            method: 'DELETE'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            alert('API call logs cleared successfully');
            loadApiCalls();
        })
        .catch(error => {
            alert(`Error clearing logs: ${error.message}`);
        });
    }
    
    function switchTab(tabId) {
        // Hide all tab panes
        document.querySelectorAll('.tab-pane').forEach(pane => {
            pane.classList.remove('active');
        });
        
        // Deactivate all tab buttons
        document.querySelectorAll('.tab-button').forEach(button => {
            button.classList.remove('active');
        });
        
        // Activate the selected tab
        document.getElementById(`${tabId}-tab`).classList.add('active');
        document.querySelector(`.tab-button[data-tab="${tabId}"]`).classList.add('active');
    }
    
    function switchModalTab(tabId) {
        // Hide all modal tab panes
        document.querySelectorAll('#request-modal .tab-pane').forEach(pane => {
            pane.classList.remove('active');
        });
        
        // Deactivate all modal tab buttons
        document.querySelectorAll('#request-modal .tab-button').forEach(button => {
            button.classList.remove('active');
        });
        
        // Activate the selected tab
        document.getElementById(`${tabId}-tab`).classList.add('active');
        document.querySelector(`#request-modal .tab-button[data-tab="${tabId}"]`).classList.add('active');
    }
    
    function formatJson(json) {
        try {
            if (typeof json === 'string') {
                json = JSON.parse(json);
            }
            return JSON.stringify(json, null, 2);
        } catch (e) {
            return json || 'No data available';
        }
    }
});
