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

/**
 * Route definitions for the User Service.
 * <p>
 * Registers the {@code /api/v1/user/**} route with a circuit breaker and a
 * {@code 503} fallback, plus a route that proxies the service's OpenAPI spec.
 */
@Configuration
public class UserServiceRoutes {

    /**
     * Routes {@code /api/v1/user/**} to the User Service, guarded by a circuit
     * breaker that forwards to the fallback route when the circuit is open.
     *
     * @return the user service router function
     */
    @Bean
    RouterFunction<ServerResponse> userRoute() {
        return route("user-service")
                .route(RequestPredicates.path("/api/v1/user/**"), http())
                .before(uri("http://localhost:8080"))
                .filter(CircuitBreakerFilterFunctions.circuitBreaker(
                        "userServiceCircuitBreaker",
                        URI.create("forward:/fallbackRoute/user")
                ))
                .build();
    }

    /**
     * Fallback route answered with {@code 503 Service Unavailable} when the user
     * service circuit is open.
     * <p>
     * Every service owns its own fallback path ({@code /fallbackRoute/<service>}):
     * sharing a single {@code /fallbackRoute} made the first registered fallback
     * answer for all of them, so an open circuit reported the wrong service.
     *
     * @return the fallback router function
     */
    @Bean
    RouterFunction<ServerResponse> userFallbackRoute() {
        return route("userFallbackRoute")
                .route(RequestPredicates.path("/fallbackRoute/user"),
        request-> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("User service is down"))
        .build();
    }

    /**
     * Proxies the User Service OpenAPI document under
     * {@code /docs/user-service/v3/api-docs} so it can be aggregated in the
     * gateway's Swagger UI.
     *
     * @return the API docs proxy router function
     */
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