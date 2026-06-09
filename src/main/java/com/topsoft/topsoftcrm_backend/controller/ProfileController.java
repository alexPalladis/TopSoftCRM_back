package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import com.topsoft.topsoftcrm_backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * PATCH /api/profile/password
     *
     * Changes the password for the currently authenticated user.
     * Works for all roles: ADMIN, NETWORK, DEALER, SUBDEALER.
     *
     * Returns { "error": "..." } on failure — same shape as GlobalExceptionHandler
     * so the frontend can always read err.response.data.error reliably.
     *
     * NOTE: Do NOT use ResponseStatusException here.
     * Spring serialises ResponseStatusException as:
     *   { "status": 400, "error": "Bad Request", "message": "..." }
     * That puts the Greek message in "message", but the frontend reads "error" first
     * and gets the useless "Bad Request" string. Return ResponseEntity directly instead.
     */
    @PatchMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal CrmUserPrincipal principal,
            @RequestBody Map<String, String> body) {

        String currentPassword = body.get("currentPassword");
        String newPassword     = body.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank() ||
                newPassword == null || newPassword.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Τα πεδία currentPassword και newPassword είναι υποχρεωτικά"));
        }

        try {
            profileService.changePassword(
                    principal.getId(),
                    principal.getRole(),
                    currentPassword,
                    newPassword
            );
        } catch (RuntimeException ex) {
            // ProfileService throws RuntimeException with a Greek message:
            //   "Λάθος τρέχον password"
            //   "Το νέο password πρέπει να έχει τουλάχιστον 6 χαρακτήρες"
            // Return it in { "error": "..." } so the frontend displays it cleanly.
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        }

        // 200 with empty body on success
        return ResponseEntity.ok().build();
    }
}