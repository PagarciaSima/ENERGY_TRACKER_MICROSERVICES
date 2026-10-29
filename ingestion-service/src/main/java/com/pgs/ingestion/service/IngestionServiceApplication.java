package com.pgs.ingestion.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bootstrap class of the Ingestion Service.
 * <p>
 * Starts the Spring Boot application that validates energy readings received
 * from IoT devices and publishes them to Kafka.
 */
@SpringBootApplication
@EnableScheduling
public class IngestionServiceApplication {

	/**
	 * Application entry point.
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(IngestionServiceApplication.class, args);
	}

}
