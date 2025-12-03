package org.auth.auth_service.controller;

import org.auth.auth_service.model.Client;
import org.auth.auth_service.repo.ClientRepository;
import org.auth.auth_service.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/oauth2")
public class TokenController {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/token")
    public ResponseEntity<?> getToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("grant_type") String grantType) {

        if (!"client_credentials".equals(grantType)) {
            return ResponseEntity.badRequest().body("Unsupported grant type");
        }

        // 1. Extract Client ID & Secret from Basic Auth Header
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing credentials");
        }

        String base64Credentials = authHeader.substring(6);
        String credentials = new String(Base64.getDecoder().decode(base64Credentials));
        String[] parts = credentials.split(":", 2);
        String clientId = parts[0];
        String clientSecret = parts[1];

        // 2. Validate Client
        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Invalid client"));

        if (!passwordEncoder.matches(clientSecret, client.getClientSecret())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid secret");
        }

        // 3. Generate Token
        try {
            List<String> scopes = Arrays.stream(client.getScopes().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            
            // We reuse generateToken but pass scopes as roles/claims
            // Ideally JwtUtil should support 'scp' or 'scope' claim for OAuth2 compliance
            String token = jwtUtil.generateToken(clientId, List.of("ROLE_SYSTEM"), scopes);

            return ResponseEntity.ok(Map.of(
                    "access_token", token,
                    "token_type", "Bearer",
                    "expires_in", client.getAccessTokenValiditySeconds()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Token generation failed");
        }
    }
}