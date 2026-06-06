package com.topsoft.topsoftcrm_backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for POST /api/webhook/payment
 *
 * Called by the invoicing app (τιμολογιέρα) when a customer makes a payment.
 * Creates a commission history entry — does NOT create or update a customer record.
 */
@Data
public class WebhookPaymentRequest {
    /** AFM of the customer who paid — must already exist in the CRM */
    private String afm;

    /** Product that was paid for (maps to Product.id 1–8) */
    private Integer productId;

    /** Gross amount paid */
    private BigDecimal amount;

    /** Date the payment was made; defaults to today if null */
    private LocalDate paymentDate;

    /** External invoice/reference number from the invoicing app */
    private String externalRef;
}
