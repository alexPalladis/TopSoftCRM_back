package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.WebhookCustomerRequest;
import com.topsoft.topsoftcrm_backend.dto.request.WebhookPaymentRequest;
import com.topsoft.topsoftcrm_backend.model.Customer;
import com.topsoft.topsoftcrm_backend.model.Dealer;
import com.topsoft.topsoftcrm_backend.model.SubDealer;
import com.topsoft.topsoftcrm_backend.repository.CustomerRepository;
import com.topsoft.topsoftcrm_backend.repository.DealerRepository;
import com.topsoft.topsoftcrm_backend.repository.ReferralCodeRepository;
import com.topsoft.topsoftcrm_backend.repository.SubDealerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final CustomerRepository       customerRepository;
    private final DealerRepository         dealerRepository;
    private final SubDealerRepository      subDealerRepository;
    private final ReferralCodeRepository   referralCodeRepository;
    private final IdGeneratorService       idGenerator;
    private final CommissionHistoryService commissionHistoryService;

    @Value("${app.webhook.secret:}")
    private String webhookSecret;

    // ─────────────────────────────────────────────────────────────────
    // Shared secret validation
    // ─────────────────────────────────────────────────────────────────
    private void validateSecret(String secret) {
        if (webhookSecret.isBlank()) return; // secret not configured — skip (dev only)
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
}