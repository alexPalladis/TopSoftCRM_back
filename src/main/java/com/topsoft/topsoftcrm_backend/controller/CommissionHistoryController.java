package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.response.CommissionHistoryResponse;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import com.topsoft.topsoftcrm_backend.service.CommissionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/commissions/history")
@RequiredArgsConstructor
public class CommissionHistoryController {

    private final CommissionHistoryService service;

    /**
     * GET /api/commissions/history
     *
     * Security rules enforced SERVER-SIDE (never trust the frontend filter):
     *  - ADMIN  → can filter by any networkId / dealerId (or none)
     *  - NETWORK → always scoped to their own networkId; any networkId param ignored
     *  - DEALER  → always scoped to their own dealerId; any dealerId param ignored
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER')")
    public ResponseEntity<PageResponse<CommissionHistoryResponse>> getAll(
            @AuthenticationPrincipal CrmUserPrincipal principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) String networkId,
            @RequestParam(required = false) String dealerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // ── Enforce scoping by role — ignore any client-supplied filter ──────
        String effectiveNetworkId = networkId;
        String effectiveDealerId  = dealerId;

        switch (principal.getRole()) {
            case "NETWORK" -> {
                // NETWORK may only ever see their own records
                effectiveNetworkId = principal.getId();
                effectiveDealerId  = null; // ignore any dealerId the frontend might pass
            }
            case "DEALER" -> {
                // DEALER may only ever see their own records
                effectiveDealerId  = principal.getId();
                effectiveNetworkId = null; // ignore any networkId the frontend might pass
            }
            // ADMIN: use params as-is
        }

        return ResponseEntity.ok(service.getAll(
                dateFrom, dateTo, productId,
                effectiveNetworkId, effectiveDealerId,
                page, size));
    }

    // ── Admin-only mutations ─────────────────────────────────────────────────

    @PatchMapping("/{id}/paid-dealer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommissionHistoryResponse> togglePaidDealer(@PathVariable Long id) {
        return ResponseEntity.ok(service.togglePaidDealer(id));
    }

    @PatchMapping("/{id}/paid-network")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommissionHistoryResponse> togglePaidNetwork(@PathVariable Long id) {
        return ResponseEntity.ok(service.togglePaidNetwork(id));
    }

    @PatchMapping("/{id}/receipt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommissionHistoryResponse> updateReceipt(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateReceipt(id, body.get("receipt")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}