package org.auth.auth_service.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import org.auth.auth_service.exception.TokenRefreshException;
import org.auth.auth_service.model.RefreshToken;
import org.auth.auth_service.model.User;
import org.auth.auth_service.repo.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;



@Service
public class RefreshTokenService {

    // safe default: 7 days (in ms) to avoid failing when external config is missing
    @Value("${jwt.refreshExpirationMs: 604800000}")
    private Long refreshTokenDurationMs;

    @Autowired
    private RefreshTokenRepository repo;

    private SecureRandom secureRandom = new SecureRandom();

    public RefreshToken createRefreshToken(User user) {
        String token = generateRandomToken();
        RefreshToken rt = new RefreshToken();
        rt.setToken(token);
        rt.setUser(user);
        rt.setCreatedAt(Instant.now());
        rt.setExpiresAt(Instant.now().plusMillis(refreshTokenDurationMs));
        rt.setRevoked(false);
        return repo.save(rt);
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public RefreshToken verifyAndConsume(String token) {
        RefreshToken rt = repo.findByToken(token)
             .orElseThrow(() -> new TokenRefreshException("Refresh token not found"));
        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenRefreshException("Token expired or revoked");
        }
        // For rotation: mark current as revoked and return it (caller will create a new one)
        rt.setRevoked(true);
        repo.save(rt);
        return rt;
    }

    public void revokeAllTokensForUser(User user) {
        repo.deleteByUser(user);
    }
}
