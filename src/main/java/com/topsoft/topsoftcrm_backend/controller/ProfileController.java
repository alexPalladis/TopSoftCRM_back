package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import com.topsoft.topsoftcrm_backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CrmUserPrincipal principal,
            @RequestBody Map<String, String> body) {

        String currentPassword = body.get("currentPassword");
        String newPassword     = body.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank() ||
                newPassword     == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Τα πεδία currentPassword και newPassword είναι υποχρεωτικά");
        }

        try {
            profileService.changePassword(
                    principal.getId(),
                    principal.getRole(),
                    currentPassword,
                    newPassword
            );
        } catch (RuntimeException ex) {
            // Service throws plain RuntimeException for "wrong current password"
            // or "too short" — surface those as 400 with the Greek message.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}