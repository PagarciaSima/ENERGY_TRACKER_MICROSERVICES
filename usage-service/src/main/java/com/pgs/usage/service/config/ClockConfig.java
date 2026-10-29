package com.pgs.usage.service.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the application {@link Clock}.
 * <p>
 * Exposing the clock as a bean keeps the time source injectable, so the alert
 * cooldown implemented in
 * {@link com.pgs.usage.service.service.UsageService} can be unit-tested with a
 * fixed clock instead of relying on the real system time.
 */
@Configuration
public class ClockConfig {

    /**
     * Creates the clock used by the service to read the current time.
     *
     * @return a UTC {@link Clock} backed by the system clock
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
