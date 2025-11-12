package org.auth.auth_service.config;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

@Configuration
public class JwksConfig {

    @Value("${jwt.keystore.path}")
    private String keyStorePath;

    @Value("${jwt.keystore.store-password}")
    private String keyStorePassword;

    @Value("${jwt.keystore.key-alias}")
    private String keyAlias;

    @Value("${jwt.keystore.key-password}")
    private String keyPassword;

    // Loads the keypair from PKCS12 keystore and builds an RSAKey (Nimbus)
    @Bean
    public RSAKey rsaKey() throws Exception {
        // Load keystore
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = new java.io.FileInputStream(keyStorePath)) {
            ks.load(is, keyStorePassword.toCharArray());
        }

        Key key = ks.getKey(keyAlias, keyPassword.toCharArray());
        if (key == null) {
            throw new IllegalStateException("No key found in keystore for alias: " + keyAlias);
        }
        if (!(key instanceof PrivateKey)) {
            throw new IllegalStateException("Key is not a private key for alias: " + keyAlias);
        }
        PrivateKey privateKey = (PrivateKey) key;

        Certificate cert = ks.getCertificate(keyAlias);
        if (cert == null) {
            throw new IllegalStateException("No certificate found for alias: " + keyAlias);
        }
        java.security.PublicKey publicKey = cert.getPublicKey();

        // Use a stable kid in production (e.g., from metadata); here we use a UUID for
        // demo.
        String kid = UUID.randomUUID().toString();

        // Build Nimbus RSAKey with both public and private parts
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) publicKey)
                .privateKey((RSAPrivateKey) privateKey)
                .keyID(kid)
                .build();

        return rsaKey;
    }

    @Bean
    public JWKSet jwkSet(RSAKey rsaKey) {
        // Expose only public JWK in the JWKSet returned by controller below
        return new JWKSet(rsaKey.toPublicJWK());
    }
}
