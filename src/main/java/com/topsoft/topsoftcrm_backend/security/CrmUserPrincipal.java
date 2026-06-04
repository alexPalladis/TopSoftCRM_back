package com.topsoft.topsoftcrm_backend.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CrmUserPrincipal {
    private final String id;
    private final String username;
    private final String role;
}