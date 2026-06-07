package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.request.CustomerRequest;
import com.topsoft.topsoftcrm_backend.dto.request.CustomerReassignRequest;
import com.topsoft.topsoftcrm_backend.dto.response.CustomerResponse;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import com.topsoft.topsoftcrm_backend.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER', 'SUBDEALER')")
    public ResponseEntity<PageResponse<CustomerResponse>> getAll(
            @AuthenticationPrincipal CrmUserPrincipal principal,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String dealerId,
            @RequestParam(required = false) String networkId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                customerService.getAll(principal, city, dealerId, networkId, active, search, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER', 'SUBDEALER')")
    public ResponseEntity<CustomerResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> create(
            @Valid @RequestBody CustomerRequest request,
            @AuthenticationPrincipal CrmUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.create(request, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CustomerRequest request,
            @AuthenticationPrincipal CrmUserPrincipal principal) {
        return ResponseEntity.ok(customerService.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserPrincipal principal) {
        customerService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reassign")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    public ResponseEntity<CustomerResponse> reassign(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserPrincipal principal,
            @Valid @RequestBody CustomerReassignRequest request) {
        return ResponseEntity.ok(customerService.reassign(id, principal, request));
    }
}