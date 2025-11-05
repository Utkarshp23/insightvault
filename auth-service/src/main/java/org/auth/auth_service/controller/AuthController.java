package org.auth.auth_service.controller;

import org.auth.auth_service.dto.AuthResponse;
import org.auth.auth_service.dto.LoginRequest;
import org.auth.auth_service.dto.SignupRequest;
import org.auth.auth_service.dto.TokenRefreshRequest;
import org.auth.auth_service.dto.TokenRefreshResponse;
import org.auth.auth_service.exception.TokenRefreshException;
import org.auth.auth_service.model.RefreshToken;
import org.auth.auth_service.model.User;
import org.auth.auth_service.repo.UserRepository;
import org.auth.auth_service.service.RefreshTokenService;
import org.auth.auth_service.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final long jwtExpirySeconds;

    @Autowired
    private RefreshTokenService refreshTokenService;

    public AuthController(UserRepository userRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
            @Value("${jwt.expiration:3600}") long jwtExpirySeconds) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtExpirySeconds = jwtExpirySeconds;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body("Email already in use");
        }
        User u = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .roles("ROLE_USER")
                .createdAt(Instant.now())
                .build();
        userRepo.save(u);
        return ResponseEntity.ok("User created");
    }

    // @PostMapping("/login")
    // public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest
    // req) {
    // User user = userRepo.findByEmail(req.getEmail()).orElseThrow(() -> new
    // RuntimeException("Invalid credentials"));
    // if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
    // throw new RuntimeException("Invalid credentials");
    // }
    // String token = jwtUtil.generateToken(user.getEmail(), List.of("ROLE_USER"));
    // return ResponseEntity.ok(new AuthResponse(token, "Bearer",
    // jwtExpirySeconds));
    // }
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        // parse roles CSV from User.roles (fallback to ROLE_USER if null/empty)
        List<String> roles;
        if (user.getRoles() == null || user.getRoles().trim().isEmpty()) {
            roles = List.of("ROLE_USER");
        } else {
            roles = Arrays.stream(user.getRoles().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        // create access token (JWT)
        String accessToken = jwtUtil.generateToken(user.getEmail(), roles);

        // create & persist refresh token (opaque string stored in DB)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // return both tokens to client
        // Reusing TokenRefreshResponse(accessToken, refreshToken) you defined earlier
        return ResponseEntity.ok(new TokenRefreshResponse(accessToken, refreshToken.getToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        }
        // authentication.getPrincipal() is the subject (we used subject as email)
        String subject = (String) authentication.getPrincipal();
        var authorities = authentication.getAuthorities().stream().map(Object::toString).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("sub", subject);
        result.put("roles", authorities);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequest request) {
        String requestToken = request.getRefreshToken(); // could come from cookie instead
        try {
            RefreshToken used = refreshTokenService.verifyAndConsume(requestToken);
            User user = used.getUser();

            List<String> roles = Arrays.stream(user.getRoles().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

            String accessToken = jwtUtil.generateToken(user.getEmail(), roles);

            // create new refresh token (rotation)
            RefreshToken newRefresh = refreshTokenService.createRefreshToken(user);
            used.setReplacedByToken(newRefresh.getToken());
            // save used token already marked revoked in verifyAndConsume

            return ResponseEntity.ok(new TokenRefreshResponse(accessToken, newRefresh.getToken()));
        } catch (TokenRefreshException e) {
            // suspicious: token not found/expired/revoked => force logout/all sessions
            // revoke
            // Consider revoking all tokens for this user if reuse detected
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }

}