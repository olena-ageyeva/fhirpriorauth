# Use an official OpenJDK image
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy Maven build files first (for caching)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# ✅ Add permission fix here
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of your code
COPY . .

# Package the application
RUN ./mvnw clean package -DskipTests

# Expose Spring Boot default port
EXPOSE 8080

# Run the app
CMD ["java", "-jar", "target/fhirpriorauth-0.0.1-SNAPSHOT.jar"]
