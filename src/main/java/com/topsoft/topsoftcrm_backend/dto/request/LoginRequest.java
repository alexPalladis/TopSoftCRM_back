package com.topsoft.topsoftcrm_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Το username είναι υποχρεωτικό")
    private String username;

    @NotBlank(message = "Το password είναι υποχρεωτικό")
    private String password;

    @NotBlank(message = "Το ID είναι υποχρεωτικό")
    @Size(min = 8, max = 8, message = "Το ID πρέπει να είναι 8 ψηφία")
    private String id;
}