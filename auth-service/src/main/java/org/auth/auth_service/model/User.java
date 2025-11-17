package org.auth.auth_service.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Column(name = "permissions", length = 1024)
  private String permissions;

  // simple roles stored as CSV "ROLE_USER,ROLE_ADMIN"
  private String roles;

  private Instant createdAt;

   public List<String> getPermissionsList() {
        if (this.permissions == null || this.permissions.isBlank()) return Collections.emptyList();
        return Arrays.stream(this.permissions.split(","))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toList());
    }
}