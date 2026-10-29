package com.pgs.user.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap class of the User Service.
 * <p>
 * Starts the Spring Boot application that exposes the REST API used to manage
 * the users of the Home Energy Tracker platform.
 */
@SpringBootApplication
public class UserServiceApplication {

	/**
	 * Application entry point.
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

}
