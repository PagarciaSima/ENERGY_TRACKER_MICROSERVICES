package com.pgs.alert.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap class of the Alert Service.
 * <p>
 * Starts the Spring Boot application that consumes alert events from Kafka,
 * notifies the affected users by email and persists an audit record.
 */
@SpringBootApplication
public class AlertServiceApplication {

	/**
	 * Application entry point.
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(AlertServiceApplication.class, args);
	}

}
