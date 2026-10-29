package com.pgs.api.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap class of the API Gateway.
 * <p>
 * Starts the Spring Boot application that acts as the single entry point of the
 * Home Energy Tracker platform, routing requests to the downstream microservices
 * and validating Keycloak JWTs.
 */
@SpringBootApplication
public class ApiGatewayApplication {

	/**
	 * Application entry point.
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
