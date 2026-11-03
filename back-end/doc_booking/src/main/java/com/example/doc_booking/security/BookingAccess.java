package com.example.doc_booking.security;
import com.example.doc_booking.model.RegisteredUser;
import com.example.doc_booking.repository.RegisteredUserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
@Component
public class BookingAccess {
    private final String secret;
    private final RegisteredUserRepository users;
    public BookingAccess(@Value("${JWT_SECRET}") String secret, RegisteredUserRepository users) { this.secret = secret; this.users = users; }
    public RegisteredUser require(String role) {
        var request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        String id;
        try { id = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).build()
            .parseSignedClaims(header.substring(7)).getPayload().getSubject(); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired login"); }
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login");
        var user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
        if (!role.equals(user.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, role + " access required");
        return user;
    }
}
