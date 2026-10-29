package com.pgs.api.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the API Gateway.
 * <p>
 * The gateway runs as an OAuth2 Resource Server: every request except the public
 * paths declared in {@code security.excluded.urls} (actuator, Swagger UI and the
 * proxied OpenAPI specs) must carry a valid Keycloak JWT. Signature validation is
 * performed against the realm's JWKS through the {@link JwtDecoder} bean.
 */
@Configuration
public class SecurityConfig {

    @Value("${keycloak.auth.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${security.excluded.urls}")
    private String[] excludedUrls;

    /**
     * Builds the security filter chain: public paths are permitted, every other
     * request must be authenticated with a valid JWT.
     *
     * @param http the Spring Security builder
     * @return the configured filter chain
     * @throws Exception if the filter chain cannot be built
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                        .requestMatchers(excludedUrls).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(Customizer.withDefaults()))
                .build();
    }

    /**
     * Creates the JWT decoder that validates tokens against the Keycloak realm's
     * JWKS endpoint.
     *
     * @return the JWKS-backed JWT decoder
     */
    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}