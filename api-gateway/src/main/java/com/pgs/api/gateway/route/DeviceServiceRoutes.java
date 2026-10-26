package com.pgs.api.gateway.route;

import org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Configuration
public class DeviceServiceRoutes {

    @Bean
    RouterFunction<ServerResponse> deviceRoute() {
        return route("device-service")
                .route(RequestPredicates.path("/api/v1/device/**"), http())
                .before(uri("http://localhost:8081"))
                .filter(CircuitBreakerFilterFunctions.circuitBreaker(
                        "deviceServiceCircuitBreaker",
                        URI.create("forward:/fallbackRoute")))
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> deviceFallbackRoute() {
        return route("fallbackRoute")
                .route(RequestPredicates.path("/fallbackRoute"),
                        request-> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body("Device service is down"))
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> deviceServiceApiDocs() {
        return GatewayRouterFunctions.route("device-service-api-docs")
                .route(RequestPredicates.path("/docs/device-service/v3/api-docs"),
                        http())
                .before(uri("http://localhost:8081"))
                .filter(setPath("/v3/api-docs"))
                .build();
    }
}