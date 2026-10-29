package com.pgs.device.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap class of the Device Service.
 * <p>
 * Starts the Spring Boot application that exposes the REST API used to manage
 * the devices of the Home Energy Tracker platform.
 */
@SpringBootApplication
public class DeviceServiceApplication {

	/**
	 * Application entry point.
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(DeviceServiceApplication.class, args);
	}

}
