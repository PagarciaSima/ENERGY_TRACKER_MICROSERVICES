package com.pgs.usage.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bootstrap class of the Usage Service.
 * <p>
 * Starts the Spring Boot application that consumes energy readings from Kafka,
 * stores them in InfluxDB and runs the scheduled aggregation / alerting task.
 */
@SpringBootApplication
@EnableScheduling
public class UsageServiceApplication {

	/**
	 * Application entry point.
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(UsageServiceApplication.class, args);
	}

}
