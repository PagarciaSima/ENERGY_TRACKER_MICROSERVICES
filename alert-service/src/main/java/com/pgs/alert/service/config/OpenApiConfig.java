package com.pgs.alert.service.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * OpenAPI / Swagger configuration for the Alert Service.
 * <p>
 * The service is a pure Kafka consumer: it does not expose REST endpoints of its
 * own, so this definition only publishes the service metadata (and the actuator
 * surface) through the generated OpenAPI document. Alerting behaviour is
 * documented as an event flow in the service README.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Alert Service API",
                version = "0.0.1-SNAPSHOT",
                description = "Microservice that consumes alert events from Kafka and notifies the affected users by email.",
                contact = @Contact(name = "Alert Service Team", email = "support@example.com"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")),
        servers = {
                @Server(url = "http://localhost:8084", description = "Local environment")
        })
public class OpenApiConfig {
}
