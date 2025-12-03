package org.proc.ai_processor_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Base64;
import java.util.Map;

@Service
public class TokenManager {

    private final RestClient authClient;
    private final String clientId;
    private final String clientSecret;
    
    private String cachedToken;
    private long tokenExpiryTime;

    public TokenManager(
            @Value("${auth-service.url:http://localhost:8081}") String authUrl,
            @Value("${ai.client-id:ai-processor-service}") String clientId,
            @Value("${ai.client-secret:ai-secret-123}") String clientSecret) {
        this.authClient = RestClient.builder().baseUrl(authUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String getAccessToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedToken;
        }
        return fetchNewToken();
    }

    private String fetchNewToken() {
        String basicAuth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

        Map response = authClient.post()
                .uri("/oauth2/token?grant_type=client_credentials")
                .header("Authorization", "Basic " + basicAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .body(Map.class);

        if (response != null && response.containsKey("access_token")) {
            this.cachedToken = (String) response.get("access_token");
            // Subtract buffer from expiry (e.g., 30s)
            int expiresIn = (int) response.get("expires_in");
            this.tokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000L) - 30000;
            return cachedToken;
        }
        throw new RuntimeException("Failed to obtain system token");
    }
}
