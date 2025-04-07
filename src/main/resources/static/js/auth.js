document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('testAuth').addEventListener('click', function() {
        const resultDiv = document.getElementById('result');
        const loadingSpan = document.getElementById('loading');

        // Show loading indicator
        loadingSpan.style.display = 'inline';

        // Clear previous results
        resultDiv.innerHTML = '';

        // Fetch authentication status
        fetch('/prior-auth/auth')
            .then(response => response.text())
            .then(data => {
                // Hide loading indicator
                loadingSpan.style.display = 'none';

                // Create result card
                const card = document.createElement('div');
                card.className = 'card';

                console.log("!!!!!!!!!!!!!!!!!!!!!!!", data)
                if (data.includes('Authentication successful')) {
                    card.classList.add('success');
                    card.innerHTML = `
                        <h2>Authentication Successful</h2>
                        <p>Successfully authenticated with Availity.</p>
                        <p><strong>Token:</strong></p>
                        <div class="token">${data.split('Token: ')[1]}</div>
                    `;
                } else {
                    card.classList.add('error');
                    card.innerHTML = `
                        <h2>Authentication Failed</h2>
                        <p>${data}</p>
                        <p>Please check your credentials and network connection.</p>
                    `;
                }

                resultDiv.appendChild(card);
            })
            .catch(error => {
                // Hide loading indicator
                loadingSpan.style.display = 'none';

                // Create error card
                const card = document.createElement('div');
                card.className = 'card error';
                card.innerHTML = `
                    <h2>Error</h2>
                    <p>An error occurred while testing authentication:</p>
                    <p>${error.message}</p>
                `;

                resultDiv.appendChild(card);
            });
    });
});
