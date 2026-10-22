package com.pgs.insight.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Insight Service.
 * <p>
 * Boots the Spring Boot application that exposes the energy insight endpoints
 * and orchestrates the usage data retrieval and the chat model interactions.
 */
@SpringBootApplication
public class InsightServiceApplication {

	/**
	 * Starts the Insight Service application.
	 *
	 * @param args command line arguments passed to the Spring context
	 */
	public static void main(String[] args) {
		SpringApplication.run(InsightServiceApplication.class, args);
	}

}
