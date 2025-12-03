package org.auth.auth_service.config;

import org.auth.auth_service.model.Client;
import org.auth.auth_service.repo.ClientRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner loadData(ClientRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.findByClientId("ai-processor-service").isEmpty()) {
                Client c = new Client();
                c.setClientId("ai-processor-service");
                c.setClientSecret(encoder.encode("ai-secret-123")); // Change in prod!
                c.setScopes("doc:read,doc:write,metadata:update");
                repo.save(c);
                System.out.println("Created OAuth2 Client: ai-processor-service");
            }
        };
    }
}
