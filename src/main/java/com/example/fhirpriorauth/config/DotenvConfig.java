package com.example.fhirpriorauth.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class to load environment variables from .env file
 */
@Configuration
public class DotenvConfig {

    private static final Logger log = LoggerFactory.getLogger(DotenvConfig.class);

    @Bean
    @Primary
    public Dotenv dotenv(ConfigurableEnvironment environment) {
        log.info("Loading environment variables from .env file");

        // Check if .env file exists
        File envFile = new File(".env");
        if (envFile.exists()) {
            log.info(".env file found at: {}", envFile.getAbsolutePath());
        } else {
            log.warn(".env file not found at: {}", envFile.getAbsolutePath());
        }

        // Load .env file
        Dotenv dotenv = Dotenv.configure()
                .directory("./") // Explicitly set the directory to the current directory
                .ignoreIfMissing()
                .load();

        // Create a map of properties from the .env file
        Map<String, Object> envProperties = new HashMap<>();

        // Add Availity credentials from .env file
        String clientId = dotenv.get("AVAILITY_CLIENT_ID");
        if (clientId != null && !clientId.isEmpty()) {
            log.info("Found AVAILITY_CLIENT_ID in .env file");
            envProperties.put("availity.client-id", clientId);

            // Also set it as a system property
            System.setProperty("availity.client-id", clientId);
        } else {
            log.warn("AVAILITY_CLIENT_ID not found in .env file");
        }

        String clientSecret = dotenv.get("AVAILITY_CLIENT_SECRET");
        if (clientSecret != null && !clientSecret.isEmpty()) {
            log.info("Found AVAILITY_CLIENT_SECRET in .env file");
            envProperties.put("availity.client-secret", clientSecret);

            // Also set it as a system property
            System.setProperty("availity.client-secret", clientSecret);
        } else {
            log.warn("AVAILITY_CLIENT_SECRET not found in .env file");
        }

        // Add default scope if not already set
        envProperties.put("availity.oauth.scope", "hipaa");
        System.setProperty("availity.oauth.scope", "hipaa");

        // Add the properties to the environment
        if (!envProperties.isEmpty()) {
            MapPropertySource propertySource = new MapPropertySource("dotenv", envProperties);
            environment.getPropertySources().addFirst(propertySource);
            log.info("Added {} properties from .env file to environment", envProperties.size());

            // Log the property names (not values for security)
            envProperties.keySet().forEach(key -> log.info("Added property: {}", key));

            // Log the current values of the properties (for debugging)
            log.debug("Current value of availity.client-id: {}", environment.getProperty("availity.client-id"));
            log.debug("Current value of availity.client-secret: {}", environment.getProperty("availity.client-secret") != null ? "[MASKED]" : "null");
        } else {
            log.warn("No properties found in .env file");
        }

        // Add an event listener to check the properties after the context is refreshed
        environment.getPropertySources().forEach(ps -> {
            log.debug("Property source: {}", ps.getName());
        });

        return dotenv;
    }

    @EventListener
    public void handleContextRefreshed(ContextRefreshedEvent event) {
        ConfigurableEnvironment env = (ConfigurableEnvironment) event.getApplicationContext().getEnvironment();
        log.info("Context refreshed, checking environment properties");
        log.info("availity.client-id: {}", env.getProperty("availity.client-id") != null ? "[SET]" : "[NOT SET]");
        log.info("availity.client-secret: {}", env.getProperty("availity.client-secret") != null ? "[SET]" : "[NOT SET]");
        log.info("availity.oauth.scope: {}", env.getProperty("availity.oauth.scope"));
    }
}
