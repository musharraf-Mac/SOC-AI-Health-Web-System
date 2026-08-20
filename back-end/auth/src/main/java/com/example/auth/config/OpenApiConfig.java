package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info().title("Auth Service API").version("1.0"))
            .components(new Components().addSecuritySchemes("ApiKeyAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-KEY")))
            .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"));
    }
}


