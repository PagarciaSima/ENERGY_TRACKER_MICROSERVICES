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
 * Route definitions for the Ingestion Service.
 * <p>
 * Registers the {@code /api/v1/ingestion/**} route with a circuit breaker and a
 * {@code 503} fallback, plus a route that proxies the service's OpenAPI spec.
 */
@Configuration
public class IngestionServiceRoutes {

    /**
     * Routes {@code /api/v1/ingestion/**} to the Ingestion Service, guarded by a
     * circuit breaker that forwards to the fallback route when the circuit is open.
     *
     * @return the ingestion service router function
     */
    @Bean
    RouterFunction<ServerResponse> ingestionRoute() {
        return route("ingestion-service")
                .route(RequestPredicates.path("/api/v1/ingestion/**"), http())
                .before(uri("http://localhost:8082"))
                .filter(CircuitBreakerFilterFunctions.circuitBreaker(
                        "ingestionServiceCircuitBreaker",
                        URI.create("forward:/fallbackRoute")
                ))
                .build();
    }

    /**
     * Fallback route answered with {@code 503 Service Unavailable} when the
     * ingestion service circuit is open.
     *
     * @return the fallback router function
     */
    @Bean
    RouterFunction<ServerResponse> ingestionFallbackRoute() {
        return route("fallbackRoute")
                .route(RequestPredicates.path("/fallbackRoute"),
                        request-> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body("Ingestion service is down"))
                .build();
    }

    /**
     * Proxies the Ingestion Service OpenAPI document under
     * {@code /docs/ingestion-service/v3/api-docs} so it can be aggregated in the
     * gateway's Swagger UI.
     *
     * @return the API docs proxy router function
     */
    @Bean
    RouterFunction<ServerResponse> ingestionServiceApiDocs() {
        return GatewayRouterFunctions.route("ingestion-service-api-docs")
                .route(RequestPredicates.path("/docs/ingestion-service/v3/api-docs"),
                        http())
                .before(uri("http://localhost:8082"))
                .filter(setPath("/v3/api-docs"))
                .build();
    }
}