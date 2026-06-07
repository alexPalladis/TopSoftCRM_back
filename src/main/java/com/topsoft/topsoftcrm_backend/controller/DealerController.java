package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.request.DealerRequest;
import com.topsoft.topsoftcrm_backend.dto.response.DealerResponse;
import com.topsoft.topsoftcrm_backend.dto.response.LookupResponse;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import com.topsoft.topsoftcrm_backend.service.DealerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dealers")
@RequiredArgsConstructor
public class DealerController {

    private final DealerService dealerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER')")
    public ResponseEntity<PageResponse<DealerResponse>> getAll(
            @AuthenticationPrincipal CrmUserPrincipal principal,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String networkId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(dealerService.getAll(principal, city, networkId, active, search, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER')")
    public ResponseEntity<DealerResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(dealerService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK')")
    public ResponseEntity<DealerResponse> create(
            @Valid @RequestBody DealerRequest request,
            @AuthenticationPrincipal CrmUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dealerService.create(request, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK')")
    public ResponseEntity<DealerResponse> update(
            @PathVariable String id,
            @Valid @RequestBody DealerRequest request,
            @AuthenticationPrincipal CrmUserPrincipal principal) {
        return ResponseEntity.ok(dealerService.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserPrincipal principal) {
        dealerService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER', 'SUBDEALER')")
    public ResponseEntity<List<LookupResponse>> getLookup(
            @AuthenticationPrincipal CrmUserPrincipal principal) {
        return ResponseEntity.ok(dealerService.getLookup(principal));
    }
}