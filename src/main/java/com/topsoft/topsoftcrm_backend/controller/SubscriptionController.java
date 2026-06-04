package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.request.SubscriptionRequest;
import com.topsoft.topsoftcrm_backend.dto.response.SubscriptionResponse;
import com.topsoft.topsoftcrm_backend.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers/{customerId}/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER', 'SUBDEALER')")
    public ResponseEntity<List<SubscriptionResponse>> getByCustomer(
            @PathVariable String customerId) {
        return ResponseEntity.ok(subscriptionService.getByCustomer(customerId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionResponse> upsert(
            @PathVariable String customerId,
            @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.upsert(customerId, request));
    }
}
