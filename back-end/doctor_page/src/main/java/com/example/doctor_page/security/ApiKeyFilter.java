package com.example.doctor_page.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {
   
    @Value("${api.key}")
    private String validApiKey;

    // Paths that should skip API key check (e.g. health checks, Swagger docs)
    private static final List<String> EXCLUDED_PATHS = List.of(
        "/swagger-ui", "/v3/api-docs", "/actuator"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean excluded = EXCLUDED_PATHS.stream().anyMatch(path::startsWith);

        if (excluded) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-KEY");

        if (apiKey == null || !apiKey.equals(validApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Missing or invalid API Key\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
