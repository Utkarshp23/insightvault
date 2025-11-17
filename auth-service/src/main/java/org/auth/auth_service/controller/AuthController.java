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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.netflix.discovery.converters.Auto;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.expiration:15000}")
    private long jwtExpirySeconds;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body("Email already in use");
        }
        String rolesCsv = "ROLE_USER";
        User u = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .roles("ROLE_USER")
                .createdAt(Instant.now())
                .build();

        List<String> scopes = List.of("doc:create", "doc:read", "doc:update", "doc:delete");
        u.setPermissions(String.join(",", scopes));
        userRepo.save(u);
        return ResponseEntity.ok("User created");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) throws JOSEException {
        System.out.println("Login attempt for user: " + req.getEmail());
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

        System.out.println();
        List<String> scopes = user.getPermissionsList();
        // create access token (JWT)
        String accessToken = jwtUtil.generateToken(user.getEmail(), roles, scopes);

        // create & persist refresh token (opaque string stored in DB)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // Set refresh token as HttpOnly cookie
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken.getToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setSecure(false);
        int refreshTokenValiditySec = 60 * 60 * 24 * 30; // e.g. 30 days
        refreshCookie.setMaxAge(refreshTokenValiditySec);
        response.addCookie(refreshCookie);

        System.out.println("Generated access token: " + accessToken);
        System.out.println("Generated refresh token: " + refreshToken.getToken());
        // return both tokens to client
        // Reusing TokenRefreshResponse(accessToken, refreshToken) you defined earlier
        return ResponseEntity.ok(new TokenRefreshResponse(accessToken, refreshToken.getToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        System.out.println("called /me endpoint");
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
    public ResponseEntity<?> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) TokenRefreshRequest request, // optional fallback
            HttpServletResponse response) {

        System.out.println("Refresh token request received"+
                (refreshTokenFromCookie != null ? " from cookie" : "") +
                (request != null && request.getRefreshToken() != null ? " from body" : ""));
        // Prefer cookie, fallback to body
        String requestToken = refreshTokenFromCookie != null ? refreshTokenFromCookie
                : (request != null ? request.getRefreshToken() : null);

        if (requestToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token");
        }

        try {
            RefreshToken used = refreshTokenService.verifyAndConsume(requestToken);
            User user = used.getUser();

            List<String> roles = Arrays.stream(user.getRoles().split(","))
                    .map(String::trim).collect(Collectors.toList());

            List<String> scopes = user.getPermissionsList();
            // create access token (JWT)
            String accessToken = jwtUtil.generateToken(user.getEmail(), roles, scopes);

            // rotation: create new refresh token
            RefreshToken newRefresh = refreshTokenService.createRefreshToken(user);
            used.setReplacedByToken(newRefresh.getToken());
            // persist changes where needed

            // Set HttpOnly refresh cookie (server-side)
            ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefresh.getToken())
                    .httpOnly(true)
                    .secure(true) // ensure HTTPS in production
                    .path("/") // adjust path if needed
                    .sameSite("Strict") // or Lax/None depending on cross-site needs
                    .maxAge(newRefresh.getExpiresAt().getEpochSecond()) // or set desired expiry
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new TokenRefreshResponse(accessToken, /* optionally null or omit refresh token */ null));
        } catch (Exception e) {
            // revoke/detect reuse as appropriate
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1) Try to read access token (optional — for audit)
        String header = request.getHeader("Authorization");
        String subjectFromAccessToken = null;
        if (header != null && header.startsWith("Bearer ")) {
            String accessToken = header.substring(7);
            System.out.println("Access token on logout: " + accessToken);
            try {
                JWTClaimsSet claims = jwtUtil.parseAndVerify(accessToken);
                subjectFromAccessToken = claims.getSubject();
            } catch (Exception ex) {
                // ignore invalid/expired access token
                ex.printStackTrace();
            }
        }

        // 2) Try to get refresh token from cookie (preferred for httpOnly refresh
        // tokens)
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("refreshToken".equals(c.getName())) {
                    refreshToken = c.getValue();
                    break;
                }
            }
        }

        // 3) Revoke refresh token if present
        if (refreshToken != null) {
            try {
                refreshTokenService.verifyAndConsume(refreshToken); // or refreshTokenService.revoke(refreshToken)
            } catch (Exception e) {
                // idempotent: swallow errors (already expired/consumed)
                e.printStackTrace();
            }
        } else if (subjectFromAccessToken != null) {
            // Optional: revoke all refresh tokens for this user
            // refreshTokenService.deleteByUserEmail(subjectFromAccessToken);
        }

        // 4) Clear refresh token cookie on client
        Cookie cleared = new Cookie("refreshToken", null);
        cleared.setHttpOnly(true);
        cleared.setPath("/"); // same path as you used for setting it
        cleared.setMaxAge(0); // delete cookie
        // secure/samesite settings as appropriate:
        // cleared.setSecure(true);
        response.addCookie(cleared);

        // 5) Response
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Logged out");
        if (subjectFromAccessToken != null)
            resp.put("user", subjectFromAccessToken);
        return ResponseEntity.ok(resp);
    }

}