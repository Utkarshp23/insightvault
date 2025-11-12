package org.auth.auth_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.JSONObjectUtils;

@RestController
public class JwksController {

    private final JWKSet jwkSet;

    public JwksController(JWKSet jwkSet) {
        this.jwkSet = jwkSet;
    }
    @GetMapping("/.well-known/jwks.json")
    public String keys() {
        System.out.println("Serving JWKS endpoint");
        // returns JSON with public keys only
        try {
            return JSONObjectUtils.toJSONString(jwkSet.toJSONObject());
        } catch (Exception e) {
            // fallback to a safe string representation in case of unexpected serialization errors
            return jwkSet.toJSONObject().toString();
        }
    }
   
}

