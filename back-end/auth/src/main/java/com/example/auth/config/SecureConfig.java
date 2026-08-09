package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecureConfig {
   @Bean
   public PasswordEncoder passwordEncoder() {
       return new BCryptPasswordEncoder();
   } 
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (important for stateless APIs)
            .csrf(csrf -> csrf.disable())
            
            // Make sure this is STATELESS (no sessions)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure authorization rules - ORDER MATTERS!
            .authorizeHttpRequests(auth -> auth
                
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/error").permitAll()  // Allow error pages
                .requestMatchers("/actuator/health").permitAll() 
                
                //Any other request - AUTHENTICATE
                .anyRequest().authenticated()
            )
            
            // Disable default login pages
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable());
            
        return http.build();
    }
}
