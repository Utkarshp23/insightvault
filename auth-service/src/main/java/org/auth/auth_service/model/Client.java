package org.auth.auth_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "oauth_clients")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Client {
    @Id
    private String clientId;
    private String clientSecret; // BCrypt encoded
    private String scopes; // Comma separated: "system:write,metadata:update"
    
    @Column(name = "access_token_validity_seconds")
    private Integer accessTokenValiditySeconds = 3600;
}
