package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.request.WebhookCustomerRequest;
import com.topsoft.topsoftcrm_backend.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/customer/register")
    public ResponseEntity<Void> customerRegister(
            @RequestBody WebhookCustomerRequest request,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {
        webhookService.handleCustomerRegister(request, secret);
        return ResponseEntity.ok().build();
    }
}