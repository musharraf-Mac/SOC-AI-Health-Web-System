package com.example.apigateway.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class CorsConfig {

    @Value("${frontend.url:https://health-web-system.netlify.app/}")
    private String frontEndProduction;

    @Value("${frontend.url.local:http://localhost:3000}")
    private String frontEndLocal;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {

        CorsConfiguration config = new CorsConfiguration();

        // Allow both production Netlify URL and localhost for development
        List<String> origins = List.of(frontEndProduction.trim().endsWith("/") ? frontEndProduction : frontEndProduction + "/",
                                       frontEndLocal.trim().endsWith("/") ? frontEndLocal : frontEndLocal + "/");

        config.setAllowedOrigins(origins);
        
        config.addAllowedOriginPattern("*");

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept", "Origin", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}