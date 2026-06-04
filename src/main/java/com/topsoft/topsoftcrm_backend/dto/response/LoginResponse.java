package com.topsoft.topsoftcrm_backend.dto.response;

import com.topsoft.topsoftcrm_backend.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LoginResponse {
    private String token;
    private String id;
    private String username;
    private UserRole role;
}
