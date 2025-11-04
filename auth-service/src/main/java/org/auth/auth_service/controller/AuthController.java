package org.auth.auth_service.controller;

import org.auth.auth_service.dto.AuthResponse;
import org.auth.auth_service.dto.LoginRequest;
import org.auth.auth_service.dto.SignupRequest;
import org.auth.auth_service.model.User;
import org.auth.auth_service.repo.UserRepository;
import org.auth.auth_service.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final long jwtExpirySeconds;

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

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepo.findByEmail(req.getEmail()).orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getEmail(), List.of("ROLE_USER"));
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", jwtExpirySeconds));
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

}