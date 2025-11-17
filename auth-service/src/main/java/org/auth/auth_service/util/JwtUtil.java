package org.auth.auth_service.util;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JwtUtil: Signs and verifies JWTs using RSA (RS256) via Nimbus.
 * - Expects an RSAKey bean (with private key) to be available in Spring context
 * (JwksConfig).
 * - Config property: jwt.expiration-seconds
 */
@Component
public class JwtUtil {

    private final RSAKey rsaKey; // should contain private key for signing
    private final long expirationSeconds;

    public JwtUtil(RSAKey rsaKey,
            @Value("${jwt.expiration-seconds:900}") long expirationSeconds) throws JOSEException {
        if (rsaKey == null || rsaKey.toPrivateKey() == null) {
            throw new IllegalArgumentException("RSAKey with private key must be provided for JwtUtil");
        }
        this.rsaKey = rsaKey;
        this.expirationSeconds = expirationSeconds;
    }

    /**
     * Generate a JWT with provided subject and roles (custom claims).
     * Signed with the private key (RS256). Header includes the kid from RSAKey.
     */
    public String generateToken(String subject, List<String> roles) throws JOSEException {
        Instant now = Instant.now();
        Date iat = Date.from(now);
        Date exp = Date.from(now.plusSeconds(expirationSeconds));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(iat)
                .expirationTime(exp)
                .claim("roles", roles)
                .issuer("http://auth-service") // adjust if you have canonical issuer
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);

        JWSSigner signer = new RSASSASigner(rsaKey.toPrivateKey());
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    public String generateToken(String subject,
            List<String> roles,
            List<String> scopes) throws JOSEException {

        Instant now = Instant.now();
        Date iat = Date.from(now);
        Date exp = Date.from(now.plusSeconds(expirationSeconds));

        // Convert scopes list -> space-separated "scope" claim (OAuth2 standard)
        String scopeString = String.join(" ", scopes);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(iat)
                .expirationTime(exp)

                // OAuth2 access token recommended claims:
                .issuer("http://auth-service") // MUST MATCH issuerUri in resource servers

                .audience(List.of("document-service", // audience required by document-service
                        "api-gateway")) // optional: token valid for gateway too

                // Authorities/permissions:
                .claim("scope", scopeString) // standard OAuth2 scope claim
                .claim("roles", roles) // keep roles if your services use it

                // Optional custom claims
                // .claim("tenant", "tenant-id-123")
                // .claim("email", "user@example.com")

                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID()) // MUST match JWKS "kid"
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);

        JWSSigner signer = new RSASSASigner(rsaKey.toPrivateKey());
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    /**
     * Convenience overload that builds roles from Spring Security UserDetails.
     */
    public String generateAccessToken(UserDetails userDetails) throws JOSEException {
        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return generateToken(userDetails.getUsername(), roles);
    }

    /**
     * Parse and verify the token signature and expiration.
     * Returns the verified JWTClaimsSet if valid, otherwise throws exception.
     */
    public JWTClaimsSet parseAndVerify(String token) throws JOSEException, ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Verify signature using the public key
        JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
        boolean signatureValid = signedJWT.verify(verifier);
        if (!signatureValid) {
            throw new JOSEException("Invalid JWT signature");
        }

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        // Manual expiration check (also can rely on downstream validators)
        Date exp = claims.getExpirationTime();
        if (exp == null) {
            throw new JOSEException("Missing exp claim in token");
        }
        if (exp.before(new Date())) {
            throw new JOSEException("Token expired at: " + exp);
        }

        return claims;
    }

    /**
     * Helper: check validity; returns true when token is valid (signature +
     * expiry).
     */
    public boolean validate(String token) {
        try {
            parseAndVerify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
