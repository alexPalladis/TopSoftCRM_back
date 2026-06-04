package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.request.CommissionRequest;
import com.topsoft.topsoftcrm_backend.dto.response.CommissionResponse;
import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import com.topsoft.topsoftcrm_backend.service.CommissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CommissionResponse>> getAllByType(
            @RequestParam EntityType entityType) {
        return ResponseEntity.ok(commissionService.getAllByType(entityType));
    }

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER', 'SUBDEALER')")
    public ResponseEntity<CommissionResponse> getByEntity(
            @PathVariable EntityType entityType,
            @PathVariable String entityId) {
        return ResponseEntity.ok(commissionService.getByEntity(entityType, entityId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommissionResponse> save(
            @Valid @RequestBody CommissionRequest request) {
        return ResponseEntity.ok(commissionService.save(request));
    }
}
