package com.pgs.api.gateway.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests that every route forwards to the URI injected from configuration.
 * <p>
 * A plain JDK {@link HttpServer} (no extra test dependency) plays the role of the
 * downstream services on a random port, and the five {@code *.service.uri} properties
 * point at it. The test therefore fails if a route still used a hardcoded
 * {@code http://localhost:<port>} URI, which is what prevents the stack from working
 * inside Docker. Security filters are disabled so the routes can be reached without a
 * Keycloak token.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Service route forwarding")
class ServiceRouteForwardingTest {

    /** Body returned by the stub downstream server for every request. */
    private static final String STUB_BODY = "stub-response";

    /** Thread pool used by the stub server. */
    private static final ExecutorService STUB_EXECUTOR = Executors.newSingleThreadExecutor();

    /** Paths received by the stub server, in order of arrival. */
    private static final List<String> RECEIVED_PATHS = new CopyOnWriteArrayList<>();

    /** Stub downstream server listening on a random free port. */
    private static final HttpServer STUB = startStub();

    @Autowired
    private MockMvc mockMvc;

    /**
     * Points every downstream service URI at the stub server.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void downstreamUris(DynamicPropertyRegistry registry) {
        final String baseUri = "http://localhost:" + STUB.getAddress().getPort();
        registry.add("user.service.uri", () -> baseUri);
        registry.add("device.service.uri", () -> baseUri);
        registry.add("ingestion.service.uri", () -> baseUri);
        registry.add("usage.service.uri", () -> baseUri);
        registry.add("insight.service.uri", () -> baseUri);
    }

    /**
     * Stops the stub server once every test has run.
     */
    @AfterAll
    static void stopStub() {
        STUB.stop(0);
        STUB_EXECUTOR.shutdownNow();
    }

    @Test
    @DisplayName("forwards /api/v1/user/** to the configured user service")
    void forwardsUserRoute() throws Exception {
        assertForwarded("/api/v1/user/2");
    }

    @Test
    @DisplayName("forwards /api/v1/device/** to the configured device service")
    void forwardsDeviceRoute() throws Exception {
        assertForwarded("/api/v1/device/1");
    }

    @Test
    @DisplayName("forwards /api/v1/ingestion/** to the configured ingestion service")
    void forwardsIngestionRoute() throws Exception {
        assertForwarded("/api/v1/ingestion/readings");
    }

    @Test
    @DisplayName("forwards /api/v1/usage/** to the configured usage service")
    void forwardsUsageRoute() throws Exception {
        assertForwarded("/api/v1/usage/2?days=3");
    }

    @Test
    @DisplayName("forwards /api/v1/insight/** to the configured insight service")
    void forwardsInsightRoute() throws Exception {
        assertForwarded("/api/v1/insight/saving-tips/2");
    }

    /**
     * Performs a GET through the gateway and asserts the stub server answered it.
     *
     * @param path gateway path to call
     * @throws Exception if the request cannot be performed
     */
    private void assertForwarded(final String path) throws Exception {
        RECEIVED_PATHS.clear();

        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().string(STUB_BODY));

        assertThat(RECEIVED_PATHS).containsExactly(path.split("\\?")[0]);
    }

    /**
     * Starts the stub downstream server on a random free port.
     *
     * @return the started server
     */
    private static HttpServer startStub() {
        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/", exchange -> {
                RECEIVED_PATHS.add(exchange.getRequestURI().getPath());
                final byte[] body = STUB_BODY.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.setExecutor(STUB_EXECUTOR);
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot start the stub downstream server", e);
        }
    }
}
