# FHIR Prior Authorization Application

A Spring Boot application that integrates with Availity's API to handle FHIR-based prior authorization requests. The application provides a web interface for submitting, tracking, and converting between FHIR and Availity API formats.

## 🚀 Live Demo

The application is deployed at: [https://fhir-prior-auth.onrender.com](https://fhir-prior-auth.onrender.com)

## 📋 Features

- **Authentication Testing**: Test Availity OAuth2 authentication
- **Prior Authorization Submission**: Submit FHIR claims for prior authorization
- **Status Polling**: Monitor the status of submitted requests
- **FHIR-Availity Converter**: Bidirectional conversion between FHIR and Availity formats
- **API Call Tracker**: Monitor external API calls with detailed logging
- **Real-time Updates**: Automatic polling for status updates

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/com/example/fhirpriorauth/
│   │   ├── config/           # Configuration classes
│   │   ├── controller/       # REST controllers
│   │   ├── model/           # Data models
│   │   ├── service/         # Business logic services
│   │   └── util/            # Utility classes and mappers
│   └── resources/
│       ├── static/          # Web assets (HTML, CSS, JS)
│       │   ├── css/         # Stylesheets
│       │   ├── js/          # JavaScript files
│       │   └── mock-data/   # Sample data files
│       └── application.properties
├── .env                     # Environment variables (not in repo)
└── pom.xml                 # Maven dependencies
```

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.2.0, Java 17+
- **Frontend**: HTML5, CSS3, Vanilla JavaScript
- **FHIR**: HAPI FHIR R4
- **Authentication**: OAuth2 (Availity)
- **Build Tool**: Maven
- **Deployment**: Render.com

## ⚙️ Setup Instructions

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Git

### 1. Clone the Repository

```bash
git clone <repository-url>
cd fhirpriorauth
```

### 2. Environment Configuration

Create a `.env` file in the project root with your Availity credentials:

```env
AVAILITY_CLIENT_ID=your_client_id_here
AVAILITY_CLIENT_SECRET=your_client_secret_here
```

**Note**: Never commit the `.env` file to version control. It's already included in `.gitignore`.

### 3. Build and Run

```bash
# Build the project
./mvnw clean compile

# Run the application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Alternative Port

If port 8080 is in use, you can specify a different port:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## 🧪 Testing the Application

### 1. Authentication Test
- Navigate to `/auth.html`
- Click "Test Authentication" to verify Availity connection

### 2. Submit Prior Authorization
- Go to `/submit.html`
- Click "Submit Prior Auth" to send a sample request
- Monitor the polling status for updates

### 3. External API Testing

Use curl to test external endpoints:

```bash
# Submit a prior authorization request
curl -X POST https://fhir-prior-auth.onrender.com/submit \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "Claim",
    "id": "example-claim-12345",
    "status": "active",
    "use": "preauthorization",
    "patient": {
      "reference": "Patient/PATIENT123",
      "display": "John Smith"
    },
    "diagnosis": [{
      "sequence": 1,
      "diagnosisCodeableConcept": {
        "coding": [{
          "system": "http://hl7.org/fhir/sid/icd-10",
          "code": "J20.9",
          "display": "Acute bronchitis"
        }]
      }
    }]
  }'

# Check status (replace RES-12345678 with actual resource ID)
curl -X GET "https://fhir-prior-auth.onrender.com/status?id=RES-12345678"
```

## 📊 API Endpoints

### Internal Endpoints
- `GET /auth.html` - Authentication test page
- `GET /submit.html` - Prior authorization submission page
- `GET /convert.html` - FHIR-Availity converter page
- `GET /api-tracker.html` - API call tracking page

### External API Endpoints
- `POST /submit` - Submit prior authorization requests
- `GET /status?id={resourceId}` - Check request status

### Utility Endpoints
- `POST /api/mapper/fhir-to-availity` - Convert FHIR to Availity format
- `POST /api/mapper/availity-to-fhir` - Convert Availity to FHIR format
- `GET /api/tracker/calls` - Retrieve API call logs

## 🔧 Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# Server configuration
server.port=8080

# Availity API configuration
availity.base-url=https://api.availity.com/availity/v1
availity.service-reviews-url=https://api.availity.com/availity/v2/service-reviews

# Logging
logging.level.com.example.fhirpriorauth=DEBUG
```

### Environment Variables

The application uses the following environment variables from `.env`:

- `AVAILITY_CLIENT_ID`: Your Availity OAuth2 client ID
- `AVAILITY_CLIENT_SECRET`: Your Availity OAuth2 client secret

## 🤝 Contributing

### Development Workflow

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes**
4. **Test thoroughly**
5. **Commit with descriptive messages**
   ```bash
   git commit -m "Add: new feature description"
   ```
6. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Create a Pull Request**

### Code Style Guidelines

- Follow Java naming conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused and concise
- Write unit tests for new functionality

### Testing

Before submitting a PR:

1. **Run all tests**
   ```bash
   ./mvnw test
   ```

2. **Test the web interface manually**
   - Verify all pages load correctly
   - Test form submissions
   - Check API endpoints

3. **Validate with external tools**
   - Test with curl commands
   - Verify JSON responses

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For questions or issues:

1. Check existing GitHub issues
2. Create a new issue with detailed description
3. Include steps to reproduce any bugs
4. Provide relevant logs and error messages

## 🔄 Deployment

The application is configured for deployment on Render.com. The deployment automatically:

- Builds the Maven project
- Starts the Spring Boot application
- Serves static content
- Handles environment variables securely

For manual deployment, ensure all environment variables are properly configured in your hosting platform.

## 📚 Additional Resources

- [FHIR R4 Documentation](https://hl7.org/fhir/R4/)
- [Availity API Documentation](https://developer.availity.com/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [HAPI FHIR Documentation](https://hapifhir.io/)