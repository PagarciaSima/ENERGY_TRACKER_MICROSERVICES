package com.pgs.api.gateway.route;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

import java.net.URI;

import org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class UserServiceRoutes {

    @Bean
    RouterFunction<ServerResponse> userRoute() {
        return route("user-service")
                .route(RequestPredicates.path("/api/v1/user/**"), http())
                .before(uri("http://localhost:8080"))
                .filter(CircuitBreakerFilterFunctions.circuitBreaker(
                        "userServiceCircuitBreaker",
                        URI.create("forward:/fallbackRoute")
                ))
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> userFallbackRoute() {
        return route("fallbackRoute")
                .route(RequestPredicates.path("/fallbackRoute"),
        request-> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("User service is down"))
        .build();
    }

    @Bean
    RouterFunction<ServerResponse> userServiceApiDocs() {
        return GatewayRouterFunctions.route("user-service-api-docs")
                .route(RequestPredicates.path("/docs/user-service/v3/api-docs"),
                        http())
                .before(uri("http://localhost:8080"))
                .filter(setPath("/v3/api-docs"))
                .build();
    }
}