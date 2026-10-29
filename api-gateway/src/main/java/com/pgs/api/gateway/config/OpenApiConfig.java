package com.pgs.api.gateway.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * OpenAPI / Swagger configuration for the API Gateway.
 * <p>
 * The gateway does not own any endpoint: it routes traffic to the downstream
 * microservices (declared as {@code RouterFunction} beans in the {@code route}
 * package) and aggregates their OpenAPI specifications in a single Swagger UI
 * (see {@code springdoc.swagger-ui.urls[*]} in {@code application.properties}).
 * This definition therefore only publishes the gateway's own metadata.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API Gateway API",
                version = "0.0.1-SNAPSHOT",
                description = "Edge service of the Home Energy Tracker platform: routes requests to the downstream "
                        + "microservices, validates Keycloak JWTs and aggregates their OpenAPI specifications.",
                contact = @Contact(name = "API Gateway Team", email = "support@example.com"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")),
        servers = {
                @Server(url = "http://localhost:9000", description = "Local environment")
        })
public class OpenApiConfig {
}
