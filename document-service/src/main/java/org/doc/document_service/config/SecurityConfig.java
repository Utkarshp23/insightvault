package org.doc.document_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${security.mock.enabled:false}")
    private boolean mockEnabled;

    /**
     * Main security filter chain:
     * - If mockEnabled: register DevAuthFilter before other auth filters.
     * - Otherwise enable oauth2 resource server (JWT).
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DevAuthFilter devAuthFilter) throws Exception {

        // Common: disable CSRF for API testing (you can enable later when needed)
        http.csrf().disable();

        // Simple route rules:
        // - POST /documents requires SCOPE_doc:create (method-level checks still apply)
        // - other endpoints require authentication
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
        );

        if (mockEnabled) {
            // Insert our dev auth filter that sets a simple Authentication when X-DEV-USER header present.
            http.addFilterBefore(devAuthFilter, UsernamePasswordAuthenticationFilter.class);
            // no resource server config in mock mode
        } else {
            // Real mode: validate JWT locally using configured jwk-set-uri
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        }

        return http.build();
    }
}
