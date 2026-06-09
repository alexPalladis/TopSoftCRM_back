package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.request.WebhookCustomerRequest;
import com.topsoft.topsoftcrm_backend.dto.request.WebhookPaymentRequest;
import com.topsoft.topsoftcrm_backend.dto.request.WebhookSubscriptionRequest;
import com.topsoft.topsoftcrm_backend.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * WebhookController
 *
 * All endpoints are called by the external invoicing app (τιμολογιέρα).
 * Authentication is via a shared secret in the X-Webhook-Secret header.
 * No JWT is required — these are machine-to-machine calls.
 *
 * Endpoints:
 *   POST /api/webhook/customer/register    — new customer self-registration
 *   POST /api/webhook/payment              — customer payment → commission history
 *   POST /api/webhook/subscription/update  — product activation / renewal / deactivation
 */
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
     *
     * Idempotent — safe to call multiple times for the same AFM.
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
     *
     * Idempotent — safe to call multiple times (creates a new commission row each time,
     * which is correct — each payment is a separate billing event).
     */
    @PostMapping("/payment")
    public ResponseEntity<Void> payment(
            @RequestBody WebhookPaymentRequest request,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {
        webhookService.handlePayment(request, secret);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/webhook/subscription/update
     *
     * Called by the invoicing app when a product subscription changes state:
     *   - Product activated for the first time       → active=true, expiryDate or quantity set
     *   - Subscription renewed                       → active=true, new expiryDate
     *   - Subscription deactivated / cancelled       → active=false
     *
     * Payload:
     *   afm        (required) — customer's tax number (must already exist in CRM)
     *   productId  (required) — 1–8 (fixed product catalogue)
     *   active     (required) — true/false
     *   expiryDate (optional) — for DATE-type products (1-5, 8)
     *   quantity   (optional) — for QUANTITY-type products (6=SMS, 7=email)
     *   cost       (optional) — if omitted, the stored cost is preserved
     *
     * Idempotent — always upserts, never duplicates.
     *
     * Example payloads:
     *
     *   // Activate "Συνδρομή εφαρμογής" (product 1) until end of 2026
     *   { "afm": "123456789", "productId": 1, "active": true, "expiryDate": "2026-12-31", "cost": 120.00 }
     *
     *   // Add 300 SMS credits (product 6)
     *   { "afm": "123456789", "productId": 6, "active": true, "quantity": 300, "cost": 30.00 }
     *
     *   // Deactivate "Σύνδεση POS" (product 3)
     *   { "afm": "123456789", "productId": 3, "active": false }
     */
    @PostMapping("/subscription/update")
    public ResponseEntity<Void> subscriptionUpdate(
            @RequestBody WebhookSubscriptionRequest request,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {
        webhookService.handleSubscriptionUpdate(request, secret);
        return ResponseEntity.ok().build();
    }
}