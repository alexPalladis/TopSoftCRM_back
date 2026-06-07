package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.repository.ReferralCodeRepository;
import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * GET /api/referral-codes/my
 *
 * Returns the active referral code for the currently authenticated
 * DEALER or SUBDEALER. Used by the Profile page to display the code
 * they hand to their customers.
 *
 * Security: only DEALER and SUBDEALER can call this endpoint.
 * ADMIN and NETWORK do not have referral codes.
 */
@RestController
@RequestMapping("/api/referral-codes")
@RequiredArgsConstructor
public class ReferralCodeController {

    private final ReferralCodeRepository referralCodeRepository;

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('DEALER', 'SUBDEALER')")
    public ResponseEntity<Map<String, String>> getMy(
            @AuthenticationPrincipal CrmUserPrincipal principal) {

        // EntityType enum values are DEALER / SUBDEALER — match the role string directly.
        // findByEntityIdAndActiveTrue returns the active code for this entity.
        return referralCodeRepository
                .findByEntityIdAndActiveTrue(principal.getId())
                .map(rc -> ResponseEntity.ok(Map.of("code", rc.getCode())))
                .orElse(ResponseEntity.ok(Map.of("code", "")));
    }
}