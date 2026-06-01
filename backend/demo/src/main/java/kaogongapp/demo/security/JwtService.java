package kaogongapp.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kaogongapp.demo.config.AppProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtService {

    private final SecretKey secretKey;
    private final long expireSeconds;

    public JwtService(AppProperties appProperties) {
        byte[] bytes = appProperties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(bytes);
        this.expireSeconds = appProperties.jwtExpireMinutes() * 60;
    }

    public String createToken(String username, String displayName, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .claims(Map.of("displayName", displayName, "role", role))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        Claims claims = parseClaims(token);
        return claims.getSubject().equals(expectedUsername) && claims.getExpiration().after(new Date());
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
