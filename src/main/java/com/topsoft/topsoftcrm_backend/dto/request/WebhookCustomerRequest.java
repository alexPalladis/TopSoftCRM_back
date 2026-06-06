package com.topsoft.topsoftcrm_backend.dto.request;

import lombok.Data;

/**
 * Payload for POST /api/webhook/customer/register
 *
 * Contains only customer identity and referral data.
 * Payment information is now handled by WebhookPaymentRequest.
 */
@Data
public class WebhookCustomerRequest {
    private String afm;
    private String eponymia;
    private String nomimosEkprosopos;
    private String epaggelma;
    private String doy;
    private String address;
    private String city;
    private String tk;
    private String phoneFixed;
    private String phoneMobile;
    private String email;
    private String referralCode;
    // NOTE: productId / amount / paymentDate / externalRef intentionally removed.
    // Payments are handled by POST /api/webhook/payment (WebhookPaymentRequest).
}