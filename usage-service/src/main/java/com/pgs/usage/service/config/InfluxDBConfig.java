package com.pgs.usage.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;

/**
 * Spring configuration for the InfluxDB client.
 * <p>
 * Creates a singleton {@link InfluxDBClient} bean that can be injected
 * wherever InfluxDB access is needed. The client is configured through the
 * following properties:
 * <ul>
 *     <li>{@code influx.url}   – the URL of the InfluxDB server (e.g. {@code http://localhost:8072})</li>
 *     <li>{@code influx.token} – the API token used to authenticate against InfluxDB</li>
 *     <li>{@code influx.org}   – the organization that owns the target bucket</li>
 * </ul>
 * <p>
 * The bucket itself is not configured here; it is usually passed per query
 * or resolved from a separate property, so the same client can write to and
 * read from multiple buckets if needed.
 */
@Configuration
public class InfluxDBConfig {

    /** URL of the InfluxDB server, injected from the {@code influx.url} property. */
    @Value("${influx.url}")
    private String influxUrl;

    /** API token used to authenticate against InfluxDB, injected from the {@code influx.token} property. */
    @Value("${influx.token}")
    private String influxToken;

    /** Organization that owns the target bucket, injected from the {@code influx.org} property. */
    @Value("${influx.org}")
    private String influxOrg;

    /**
     * Creates the singleton {@link InfluxDBClient} used by the application.
     * <p>
     * The client is thread-safe and designed to be shared across the whole
     * application, so it is registered as a Spring bean instead of being
     * instantiated on each use.
     *
     * @return a fully configured {@link InfluxDBClient} pointing to the
     *         server, organization and token defined by the {@code influx.*}
     *         properties
     */
    @Bean
    InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(influxUrl, influxToken.toCharArray(), influxOrg);
    }
}