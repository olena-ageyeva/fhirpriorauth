package com.example.fhirpriorauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FhirPriorAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(FhirPriorAuthApplication.class, args);
	}

}
