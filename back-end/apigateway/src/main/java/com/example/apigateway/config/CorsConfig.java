package com.example.apigateway.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {

        CorsConfiguration config = new CorsConfiguration();

        // Read FRONT_END from environment variable (set in docker-compose or CI/CD)
        String productionUrl = System.getenv("FRONT_END");
        String developmentUrl = "http://localhost:3000";  // Default for local development

        // Build list of allowed origins
        List<String> origins = new java.util.ArrayList<>();

        if (productionUrl != null && !productionUrl.trim().isEmpty()) {
            origins.add(productionUrl.trim().endsWith("/") ? productionUrl : productionUrl + "/");
        }

        origins.add(developmentUrl);

        // Optionally add wildcard for development testing (remove this line in strict production)
        // Uncomment the next line if you need to allow any origin during development:
        // origins.add("*");

        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept", "Origin", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}