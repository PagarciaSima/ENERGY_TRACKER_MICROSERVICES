package com.pgs.insight.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration for the outbound {@link RestClient} HTTP clients used
 * to communicate with other microservices.
 * <p>
 * Each remote service gets its own {@code RestClient} bean with its base URL
 * pre-configured, so callers can issue relative requests without repeating the
 * host details. The base URLs are read from properties, which allows different
 * values per environment (local, Docker, production):
 * <ul>
 *     <li>{@code usage.service.url} – the Usage Service API root
 *         (e.g. {@code http://localhost:8083/api/v1/usage})</li>
 * </ul>
 * <p>
 * The beans are built from the auto-configured {@link RestClient.Builder}
 * provided by Spring Boot, so application-wide customizers (message converters,
 * observability, SSL, etc.) are applied consistently.
 */
@Configuration
public class RestClientConfig {

	/**
	 * Creates the {@link RestClient} used to talk to the Usage Service.
	 *
	 * @param builder        the auto-configured {@link RestClient.Builder}
	 * @param usageServiceUrl base URL of the Usage Service, injected from the
	 *                        {@code usage.service.url} property
	 * @return a {@code RestClient} with the Usage Service base URL applied
	 */
	@Bean
	RestClient usageRestClient(RestClient.Builder builder,
							   @Value("${usage.service.url}") String usageServiceUrl) {
		return builder.baseUrl(usageServiceUrl).build();
	}
}
