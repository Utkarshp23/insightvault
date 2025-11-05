package org.auth.auth_service.repo;

import java.util.Optional;

import org.auth.auth_service.model.RefreshToken;
import org.auth.auth_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
}

