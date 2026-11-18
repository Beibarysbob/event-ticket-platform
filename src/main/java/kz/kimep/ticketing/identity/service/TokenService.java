package kz.kimep.ticketing.identity.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import kz.kimep.ticketing.shared.domain.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public TokenService(@Value("${ticketing.jwt.secret}") String secret,
                        @Value("${ticketing.jwt.access-token-expiration}") Duration accessTtl,
                        @Value("${ticketing.jwt.refresh-token-expiration}") Duration refreshTtl) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 characters");
        }
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String accessToken(UserAccount user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getEmail()).claim("role", user.getRole())
            .issuedAt(Date.from(now)).expiration(Date.from(now.plus(accessTtl)))
            .signWith(key).compact();
    }

    public String emailFromAccess(String jwt) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt)
            .getPayload().getSubject();
    }

    public String newRefreshToken() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }

    public Instant refreshExpiry() {
        return Instant.now().plus(refreshTtl);
    }

    public String digest(String token) {
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
