package org.auth.auth_service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SignupRequest {
  @Email 
  @NotBlank 
  private String email;

  @NotBlank     
  @Size(min=6) 
  private String password;
}
