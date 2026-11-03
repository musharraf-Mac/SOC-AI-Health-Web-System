package com.example.apigateway;

import com.example.apigateway.security.JwtAuthFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingGatewayAuthTest {
    @Test void doctorAndBookingRoutesRequireValidBearerTokenBeforeForwarding() {
        var filter = new JwtAuthFilter();
        String key = "test-only-gateway-key-at-least-32-characters";
        ReflectionTestUtils.setField(filter, "secret", key);
        for (String path : List.of("/api/doctor/patients", "/api/doctor/prescriptions/p@example.test", "/api/doc_booking/doctor/me/availability", "/api/doc_booking/doctors")) {
            var chain = mock(GatewayFilterChain.class);
            var anonymous = MockServerWebExchange.from(MockServerHttpRequest.get(path));
            filter.filter(anonymous, chain).block();
            assertEquals(HttpStatus.UNAUTHORIZED, anonymous.getResponse().getStatusCode()); verifyNoInteractions(chain);
            var invalid = MockServerWebExchange.from(MockServerHttpRequest.get(path).header("Authorization", "Bearer invalid"));
            filter.filter(invalid, chain).block(); assertEquals(HttpStatus.UNAUTHORIZED, invalid.getResponse().getStatusCode()); verifyNoInteractions(chain);
            String token = Jwts.builder().subject("user").signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact();
            var signed = MockServerWebExchange.from(MockServerHttpRequest.get(path).header("Authorization", "Bearer " + token));
            when(chain.filter(signed)).thenReturn(Mono.empty()); filter.filter(signed, chain).block(); verify(chain).filter(signed);
        }
    }
}
