package com.pgs.insight.service.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * OpenAPI / Swagger configuration for the Insight Service.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Insight Service API",
                version = "0.0.1-SNAPSHOT",
                description = "Microservice that generates energy insights and saving tips for the users of the Home Energy Tracker platform.",
                contact = @Contact(name = "Insight Service Team", email = "support@example.com"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")),
        servers = {
                @Server(url = "http://localhost:8085", description = "Local environment")
        })
public class OpenApiConfig {
}