package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import com.topsoft.topsoftcrm_backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
        profileService.changePassword(
                principal.getId(),
                principal.getRole(),
                body.get("currentPassword"),
                body.get("newPassword")
        );
        return ResponseEntity.ok().build();
    }
}
