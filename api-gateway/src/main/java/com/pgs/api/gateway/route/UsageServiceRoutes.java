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

/**
 * Route definitions for the Usage Service.
 * <p>
 * Registers the {@code /api/v1/usage/**} route with a circuit breaker and a
 * {@code 503} fallback, plus a route that proxies the service's OpenAPI spec.
 */
@Configuration
public class UsageServiceRoutes {

    /**
     * Routes {@code /api/v1/usage/**} to the Usage Service, guarded by a circuit
     * breaker that forwards to the fallback route when the circuit is open.
     *
     * @return the usage service router function
     */
    @Bean
    RouterFunction<ServerResponse> usageRoute() {
        return route("usage-service")
                .route(RequestPredicates.path("/api/v1/usage/**"), http())
                .before(uri("http://localhost:8083"))
                .filter(CircuitBreakerFilterFunctions.circuitBreaker(
                        "usageServiceCircuitBreaker",
                        URI.create("forward:/fallbackRoute/usage")
                ))
                .build();
    }

    /**
     * Fallback route answered with {@code 503 Service Unavailable} when the usage
     * service circuit is open.
     * <p>
     * Every service owns its own fallback path ({@code /fallbackRoute/<service>}):
     * sharing a single {@code /fallbackRoute} made the first registered fallback
     * answer for all of them, so an open circuit reported the wrong service.
     *
     * @return the fallback router function
     */
    @Bean
    RouterFunction<ServerResponse> usageFallbackRoute() {
        return route("usageFallbackRoute")
                .route(RequestPredicates.path("/fallbackRoute/usage"),
                        request-> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body("Usage service is down"))
                .build();
    }

    /**
     * Proxies the Usage Service OpenAPI document under
     * {@code /docs/usage-service/v3/api-docs} so it can be aggregated in the
     * gateway's Swagger UI.
     *
     * @return the API docs proxy router function
     */
    @Bean
    RouterFunction<ServerResponse> usageServiceApiDocs() {
        return GatewayRouterFunctions.route("usage-service-api-docs")
                .route(RequestPredicates.path("/docs/usage-service/v3/api-docs"),
                        http())
                .before(uri("http://localhost:8083"))
                .filter(setPath("/v3/api-docs"))
                .build();
    }
}
