package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.request.WebhookCustomerRequest;
import com.topsoft.topsoftcrm_backend.dto.request.WebhookPaymentRequest;
import com.topsoft.topsoftcrm_backend.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * POST /api/webhook/customer/register
     *
     * Called by the invoicing app when a new customer self-registers.
     * Creates the customer record and links them to the correct dealer/subdealer
     * via the referral code included in the payload.
     */
    @PostMapping("/customer/register")
    public ResponseEntity<Void> customerRegister(
            @RequestBody WebhookCustomerRequest request,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {
        webhookService.handleCustomerRegister(request, secret);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/webhook/payment
     *
     * Called by the invoicing app when a customer pays for a product.
     * Creates a commission history entry — does NOT touch the customer record.
     * The customer must already exist (registration webhook must have run first).
     */
    @PostMapping("/payment")
    public ResponseEntity<Void> payment(
            @RequestBody WebhookPaymentRequest request,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {
        webhookService.handlePayment(request, secret);
        return ResponseEntity.ok().build();
    }
}