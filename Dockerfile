# Use OpenJDK image
FROM eclipse-temurin:17-jdk

# Set the working directory
WORKDIR /app

# Copy Maven build files
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies (helps with caching)
RUN ./mvnw dependency:go-offline

# Copy the rest of the code
COPY src ./src

# Package the application
RUN ./mvnw clean package -DskipTests

# Run the application
CMD ["java", "-jar", "target/fhirpriorauth-0.0.1-SNAPSHOT.jar"]
