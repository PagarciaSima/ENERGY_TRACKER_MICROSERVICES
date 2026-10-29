package com.pgs.api.gateway.route;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests for the circuit-breaker fallback routes of the gateway.
 * <p>
 * Every service registers its own fallback path ({@code /fallbackRoute/<service>}).
 * They used to share a single {@code /fallbackRoute}, which made the first
 * registered fallback answer for all of them: an open circuit for the insight
 * service replied {@code "Device service is down"}. These tests exercise the
 * router functions directly through {@link MockMvc} (no HTTP server, no Keycloak
 * and no downstream services required).
 */
@DisplayName("Circuit breaker fallback routes")
class ServiceFallbackRoutesTest {

    private MockMvc mockMvc;

    /**
     * Builds a {@link MockMvc} instance with the five fallback router functions
     * registered, exactly as the application context does.
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.routerFunctions(
                new UserServiceRoutes().userFallbackRoute(),
                new DeviceServiceRoutes().deviceFallbackRoute(),
                new IngestionServiceRoutes().ingestionFallbackRoute(),
                new InsightServiceRoutes().insightFallbackRoute(),
                new UsageServiceRoutes().usageFallbackRoute())
                .build();
    }

    @Nested
    @DisplayName("fallback paths")
    class FallbackPaths {

        @Test
        @DisplayName("answer 503 with the user service message on /fallbackRoute/user")
        void answersUserFallback() throws Exception {
            mockMvc.perform(get("/fallbackRoute/user"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string("User service is down"));
        }

        @Test
        @DisplayName("answer 503 with the device service message on /fallbackRoute/device")
        void answersDeviceFallback() throws Exception {
            mockMvc.perform(get("/fallbackRoute/device"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string("Device service is down"));
        }

        @Test
        @DisplayName("answer 503 with the ingestion service message on /fallbackRoute/ingestion")
        void answersIngestionFallback() throws Exception {
            mockMvc.perform(get("/fallbackRoute/ingestion"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string("Ingestion service is down"));
        }

        @Test
        @DisplayName("answer 503 with the insight service message on /fallbackRoute/insight")
        void answersInsightFallback() throws Exception {
            mockMvc.perform(get("/fallbackRoute/insight"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string("Insight service is down"));
        }

        @Test
        @DisplayName("answer 503 with the usage service message on /fallbackRoute/usage")
        void answersUsageFallback() throws Exception {
            mockMvc.perform(get("/fallbackRoute/usage"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string("Usage service is down"));
        }
    }

    @Nested
    @DisplayName("shared legacy path")
    class SharedLegacyPath {

        @Test
        @DisplayName("is no longer registered, so a fallback never reports the wrong service")
        void isNoLongerRegistered() throws Exception {
            mockMvc.perform(get("/fallbackRoute"))
                    .andExpect(status().isNotFound());
        }
    }
}
