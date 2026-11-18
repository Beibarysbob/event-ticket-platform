package kz.kimep.ticketing.identity.service;

import java.time.Instant;
import kz.kimep.ticketing.identity.api.AuthDtos;
import kz.kimep.ticketing.shared.domain.RefreshToken;
import kz.kimep.ticketing.shared.domain.UserAccount;
import kz.kimep.ticketing.shared.error.DomainException;
import kz.kimep.ticketing.shared.repo.RefreshTokenRepository;
import kz.kimep.ticketing.shared.repo.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdentityService {
    private final UserAccountRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    @Transactional
    public AuthDtos.Tokens register(AuthDtos.Register request) {
        if (users.findByEmail(request.email().trim().toLowerCase()).isPresent()) {
            throw new DomainException(HttpStatus.CONFLICT, "Email is already registered");
        }
        UserAccount user = new UserAccount();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(encoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole("CUSTOMER");
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        return issue(users.save(user));
    }

    @Transactional
    public AuthDtos.Tokens login(AuthDtos.Login request) {
        UserAccount user = users.findByEmail(request.email().trim().toLowerCase())
            .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "Incorrect email or password"));
        if (!user.isEnabled() || !encoder.matches(request.password(), user.getPasswordHash())) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
        }
        return issue(user);
    }

    @Transactional
    public AuthDtos.Tokens refresh(AuthDtos.Refresh request) {
        RefreshToken stored = refreshTokens.findByToken(tokens.digest(request.refreshToken()))
            .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "Refresh token not valid"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }
        stored.setRevoked(true);
        return issue(stored.getUser());
    }

    @Transactional
    public void logout(AuthDtos.Refresh request, Long userId) {
        refreshTokens.findByToken(tokens.digest(request.refreshToken())).ifPresent(stored -> {
            if (stored.getUser().getId().equals(userId)) {
                stored.setRevoked(true);
            }
        });
    }

    private AuthDtos.Tokens issue(UserAccount user) {
        String refresh = tokens.newRefreshToken();
        RefreshToken saved = new RefreshToken();
        saved.setUser(user);
        saved.setToken(tokens.digest(refresh));
        saved.setExpiresAt(tokens.refreshExpiry());
        refreshTokens.save(saved);
        return new AuthDtos.Tokens(tokens.accessToken(user), refresh, "Bearer");
    }
}
