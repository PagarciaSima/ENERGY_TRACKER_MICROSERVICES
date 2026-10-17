package com.pgs.usage.service.config;

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
 *     <li>{@code device.service.url} – the Device Service API root
 *         (e.g. {@code http://localhost:8081/api/v1/device})</li>
 *     <li>{@code user.service.url} – the User Service API root
 *         (e.g. {@code http://localhost:8080/api/v1/user})</li>
 * </ul>
 * <p>
 * The beans are built from the auto-configured {@link RestClient.Builder}
 * provided by Spring Boot, so application-wide customizers (message converters,
 * observability, SSL, etc.) are applied consistently.
 */
@Configuration
public class RestClientConfig {

    /**
     * Creates the {@link RestClient} used to talk to the Device Service.
     *
     * @param builder          the auto-configured {@link RestClient.Builder}
     * @param deviceServiceUrl base URL of the Device Service, injected from the
     *                         {@code device.service.url} property
     * @return a {@code RestClient} with the Device Service base URL applied
     */
    @Bean
    RestClient deviceRestClient(RestClient.Builder builder,
                                @Value("${device.service.url}") String deviceServiceUrl) {
        return builder.baseUrl(deviceServiceUrl).build();
    }

    /**
     * Creates the {@link RestClient} used to talk to the User Service.
     *
     * @param builder        the auto-configured {@link RestClient.Builder}
     * @param userServiceUrl base URL of the User Service, injected from the
     *                       {@code user.service.url} property
     * @return a {@code RestClient} with the User Service base URL applied
     */
    @Bean
    RestClient userRestClient(RestClient.Builder builder,
                              @Value("${user.service.url}") String userServiceUrl) {
        return builder.baseUrl(userServiceUrl).build();
    }
}
