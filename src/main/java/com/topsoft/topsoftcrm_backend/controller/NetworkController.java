package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.request.NetworkRequest;
import com.topsoft.topsoftcrm_backend.dto.response.NetworkResponse;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import com.topsoft.topsoftcrm_backend.service.NetworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/networks")
@RequiredArgsConstructor
public class NetworkController {

    private final NetworkService networkService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK')")
    public ResponseEntity<PageResponse<NetworkResponse>> getAll(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(networkService.getAll(city, active, search, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER')")
    public ResponseEntity<NetworkResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(networkService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NetworkResponse> create(
            @Valid @RequestBody NetworkRequest request,
            @AuthenticationPrincipal CrmUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(networkService.create(request, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NetworkResponse> update(
            @PathVariable String id,
            @Valid @RequestBody NetworkRequest request,
            @AuthenticationPrincipal CrmUserPrincipal principal) {
        return ResponseEntity.ok(networkService.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserPrincipal principal) {
        networkService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}