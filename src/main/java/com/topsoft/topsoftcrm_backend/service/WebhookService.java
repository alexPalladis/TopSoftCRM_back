package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.WebhookCustomerRequest;
import com.topsoft.topsoftcrm_backend.dto.request.WebhookPaymentRequest;
import com.topsoft.topsoftcrm_backend.dto.request.WebhookSubscriptionRequest;
import com.topsoft.topsoftcrm_backend.model.Customer;
import com.topsoft.topsoftcrm_backend.model.Dealer;
import com.topsoft.topsoftcrm_backend.model.Product;
import com.topsoft.topsoftcrm_backend.model.SubDealer;
import com.topsoft.topsoftcrm_backend.model.Subscription;
import com.topsoft.topsoftcrm_backend.repository.CustomerRepository;
import com.topsoft.topsoftcrm_backend.repository.DealerRepository;
import com.topsoft.topsoftcrm_backend.repository.ProductRepository;
import com.topsoft.topsoftcrm_backend.repository.ReferralCodeRepository;
import com.topsoft.topsoftcrm_backend.repository.SubDealerRepository;
import com.topsoft.topsoftcrm_backend.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final CustomerRepository       customerRepository;
    private final DealerRepository         dealerRepository;
    private final SubDealerRepository      subDealerRepository;
    private final ReferralCodeRepository   referralCodeRepository;
    private final SubscriptionRepository   subscriptionRepository;
    private final ProductRepository        productRepository;
    private final IdGeneratorService       idGenerator;
    private final CommissionHistoryService commissionHistoryService;

    @Value("${app.webhook.secret:}")
    private String webhookSecret;

    // ─────────────────────────────────────────────────────────────────
    // Shared secret validation
    // ─────────────────────────────────────────────────────────────────
    private void validateSecret(String secret) {
        if (webhookSecret.isBlank()) return; // not configured — skip (dev only)
        if (secret == null || !webhookSecret.equals(secret)) {
            log.warn("Webhook: μη εξουσιοδοτημένο αίτημα — λάθος ή κενό secret");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Μη εξουσιοδοτημένο webhook αίτημα");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /api/webhook/customer/register
    //
    // Creates a new customer record when a user self-registers in the
    // invoicing app (τιμολογιέρα). The referral code determines which
    // dealer / subdealer the customer belongs to.
    // ─────────────────────────────────────────────────────────────────
    @Transactional
    public void handleCustomerRegister(WebhookCustomerRequest request, String secret) {
        validateSecret(secret);

        // Idempotent — skip silently if this AFM already exists
        if (customerRepository.existsByAfm(request.getAfm())) {
            log.info("Webhook register: πελάτης με ΑΦΜ {} υπάρχει ήδη — παράλειψη",
                    request.getAfm());
            return;
        }

        // Resolve dealer / subdealer from referral code
        Dealer    dealer    = null;
        SubDealer subDealer = null;

        if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
            var referral = referralCodeRepository
                    .findByCodeAndActiveTrue(request.getReferralCode())
                    .orElse(null);

            if (referral != null) {
                switch (referral.getEntityType().name()) {
                    case "DEALER" ->
                            dealer = dealerRepository
                                    .findById(referral.getEntityId()).orElse(null);
                    case "SUBDEALER" -> {
                        subDealer = subDealerRepository
                                .findById(referral.getEntityId()).orElse(null);
                        if (subDealer != null) dealer = subDealer.getDealer();
                    }
                }
            }
        }

        if (dealer == null) {
            log.warn("Webhook register: δεν βρέθηκε dealer για referral code '{}' — αδυναμία καταχώρησης",
                    request.getReferralCode());
            return;
        }

        Customer customer = Customer.builder()
                .id(idGenerator.generateCustomerId())
                .afm(request.getAfm())
                .eponymia(request.getEponymia())
                .nomimosEkprosopos(request.getNomimosEkprosopos())
                .epaggelma(request.getEpaggelma() != null ? request.getEpaggelma() : "—")
                .doy(request.getDoy()         != null ? request.getDoy()         : "—")
                .address(request.getAddress() != null ? request.getAddress()     : "—")
                .city(request.getCity()       != null ? request.getCity()        : "—")
                .tk(request.getTk()           != null ? request.getTk()          : "00000")
                .phoneFixed(request.getPhoneFixed())
                .phoneMobile(request.getPhoneMobile())
                .email(request.getEmail())
                .active(true)
                .dealer(dealer)
                .subDealer(subDealer)
                // network is NOT stored — always derived via dealer.getNetwork() at read time
                .source("API")
                .referralCode(request.getReferralCode())
                .build();

        customerRepository.save(customer);
        log.info("Webhook register: νέος πελάτης {} καταχωρήθηκε επιτυχώς (dealer={})",
                request.getAfm(), dealer.getId());
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /api/webhook/payment
    //
    // Records a commission entry when a customer pays for a product.
    // The customer must already exist in the CRM. This endpoint does
    // NOT create or modify any customer record.
    // ─────────────────────────────────────────────────────────────────
    @Transactional
    public void handlePayment(WebhookPaymentRequest request, String secret) {
        validateSecret(secret);

        if (request.getAfm() == null || request.getAfm().isBlank()) {
            log.warn("Webhook payment: κενό ΑΦΜ — παράλειψη");
            return;
        }
        if (request.getProductId() == null || request.getAmount() == null) {
            log.warn("Webhook payment: λείπει productId ή amount για ΑΦΜ {} — παράλειψη",
                    request.getAfm());
            return;
        }

        // Customer must exist — if not, it means the registration webhook failed
        // or was not called. Log a warning but do not throw (idempotent behaviour).
        if (!customerRepository.existsByAfm(request.getAfm())) {
            log.warn("Webhook payment: πελάτης με ΑΦΜ {} δεν βρέθηκε — δεν καταχωρήθηκε προμήθεια",
                    request.getAfm());
            return;
        }

        commissionHistoryService.createFromPayment(
                request.getAfm(),
                request.getProductId(),
                request.getAmount(),
                request.getPaymentDate(),
                request.getExternalRef()
        );

        log.info("Webhook payment: προμήθεια καταχωρήθηκε για ΑΦΜ {} / productId {}",
                request.getAfm(), request.getProductId());
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /api/webhook/subscription/update
    //
    // Activates, renews, or deactivates a product subscription for a
    // customer. Called by the invoicing app when:
    //   - A customer purchases / activates a product
    //   - A subscription is renewed (new expiry date)
    //   - A subscription is deactivated / cancelled
    //
    // This endpoint is IDEMPOTENT — it upserts the subscription row.
    // Calling it multiple times with the same data is safe.
    //
    // The customer must already exist in the CRM (register webhook must
    // have run first). If the customer is not found the call is silently
    // ignored so the invoicing app never gets a hard error from us.
    // ─────────────────────────────────────────────────────────────────
    @Transactional
    public void handleSubscriptionUpdate(WebhookSubscriptionRequest request, String secret) {
        validateSecret(secret);

        // ── Validate required fields ──────────────────────────────────────────
        if (request.getAfm() == null || request.getAfm().isBlank()) {
            log.warn("Webhook subscription: κενό ΑΦΜ — παράλειψη");
            return;
        }
        if (request.getProductId() == null) {
            log.warn("Webhook subscription: λείπει productId για ΑΦΜ {} — παράλειψη",
                    request.getAfm());
            return;
        }
        if (request.getActive() == null) {
            log.warn("Webhook subscription: λείπει active flag για ΑΦΜ {} / productId {} — παράλειψη",
                    request.getAfm(), request.getProductId());
            return;
        }

        // ── Look up customer ──────────────────────────────────────────────────
        Customer customer = customerRepository.findByAfm(request.getAfm()).orElse(null);
        if (customer == null) {
            log.warn("Webhook subscription: πελάτης με ΑΦΜ {} δεν βρέθηκε — παράλειψη",
                    request.getAfm());
            return;
        }

        // ── Look up product ───────────────────────────────────────────────────
        Product product = productRepository.findById(request.getProductId()).orElse(null);
        if (product == null) {
            log.warn("Webhook subscription: productId {} δεν βρέθηκε — παράλειψη",
                    request.getProductId());
            return;
        }

        // ── Upsert the subscription row ───────────────────────────────────────
        Subscription sub = subscriptionRepository
                .findByCustomerIdAndProductId(customer.getId(), request.getProductId())
                .orElse(null);

        if (sub == null) {
            // CREATE — first time this product is activated for this customer
            sub = Subscription.builder()
                    .customer(customer)
                    .product(product)
                    .active(request.getActive())
                    .expiryDate(request.getExpiryDate())
                    .quantity(request.getQuantity())
                    .cost(resolveCost(request.getCost(), product))
                    .activatedAt(request.getActive() ? LocalDateTime.now() : null)
                    .updatedAt(LocalDateTime.now())
                    .build();
            subscriptionRepository.save(sub);
            log.info("Webhook subscription: ΔΗΜΙΟΥΡΓΙΑ — ΑΦΜ {} / productId {} / active={}",
                    request.getAfm(), request.getProductId(), request.getActive());
        } else {
            // UPDATE — subscription already exists, update fields
            sub.setActive(request.getActive());
            sub.setExpiryDate(request.getExpiryDate());
            sub.setQuantity(request.getQuantity());
            sub.setUpdatedAt(LocalDateTime.now());

            // Only update cost if explicitly provided in the webhook payload
            if (request.getCost() != null) {
                sub.setCost(request.getCost());
            }

            // Update activatedAt only when transitioning to active
            if (Boolean.TRUE.equals(request.getActive()) && !Boolean.TRUE.equals(sub.getActive())) {
                sub.setActivatedAt(LocalDateTime.now());
            }

            subscriptionRepository.save(sub);
            log.info("Webhook subscription: ΕΝΗΜΕΡΩΣΗ — ΑΦΜ {} / productId {} / active={}",
                    request.getAfm(), request.getProductId(), request.getActive());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Resolves the cost for a subscription.
     * Prefers the explicitly provided cost; falls back to the product's
     * default price so the subscription row always has a non-null cost.
     */
    private BigDecimal resolveCost(BigDecimal requestedCost, Product product) {
        if (requestedCost != null) return requestedCost;
        return product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
    }
}