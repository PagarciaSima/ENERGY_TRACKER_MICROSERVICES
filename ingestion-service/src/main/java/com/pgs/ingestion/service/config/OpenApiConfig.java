package com.pgs.ingestion.service.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * OpenAPI / Swagger configuration for the Device Service.   
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Ingestion Service API",                    
                version = "0.0.1-SNAPSHOT",
                description = "Microservice that manages the devices of the Home Energy Tracker platform.",
                contact = @Contact(name = "ingestion Service Team", 
                                   email = "support@example.com"),
                license = @License(name = "Apache 2.0", url = "...")),
        servers = {
                @Server(url = "http://localhost:8082", description = "Local environment")  
        })
public class OpenApiConfig {
}