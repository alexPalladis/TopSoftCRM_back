package com.topsoft.topsoftcrm_backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for POST /api/webhook/subscription/update
 *
 * Called by the invoicing app (τιμολογιέρα) when a customer activates,
 * renews, or deactivates a product subscription.
 *
 * Rules:
 *  - afm        : required — must match an existing customer in the CRM
 *  - productId  : required — 1-8 (the fixed product list)
 *  - active     : required — true = activated/renewed, false = deactivated
 *  - expiryDate : for DATE-type products (1-5, 8); null for QUANTITY-type
 *  - quantity   : for QUANTITY-type products (6=SMS, 7=email); null for DATE-type
 *  - cost       : optional — if provided, overrides the stored cost for this subscription
 *
 * The webhook is idempotent: calling it multiple times with the same data
 * is safe — it will upsert (create or update) the subscription row.
 */
@Data
public class WebhookSubscriptionRequest {

    /** AFM of the customer — must already exist in the CRM */
    private String afm;

    /** Product ID 1–8 */
    private Integer productId;

    /** true = active/renewed, false = deactivated */
    private Boolean active;

    /**
     * For DATE-type products (1=Συνδρομή εφαρμογής, 2=Ενεργός Πάροχος ΗΤ,
     * 3=Σύνδεση POS, 4=Άδεια mobile App, 5=Σύνδεση WooCommerce,
     * 8=Ψηφιακό Πελατολόγιο).
     * Null for QUANTITY-type products.
     */
    private LocalDate expiryDate;

    /**
     * For QUANTITY-type products (6=Ενεργά SMS, 7=Ενεργά email).
     * Represents the number of SMS/emails purchased (e.g. 300, 500).
     * Null for DATE-type products.
     */
    private Integer quantity;

    /**
     * Optional cost override. If null the existing stored cost is kept
     * (or the product's default price is used for new subscriptions).
     */
    private BigDecimal cost;
}