package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.WebhookCustomerRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final CustomerRepository customerRepository;
    private final DealerRepository dealerRepository;
    private final SubDealerRepository subDealerRepository;
    private final ReferralCodeRepository referralCodeRepository;
    private final IdGeneratorService   idGenerator;

    @Value("${app.webhook.secret:}")
    private String webhookSecret;

    @Transactional
    public void handleCustomerRegister(WebhookCustomerRequest request, String secret) {
        // Έλεγχος secret
        if (!webhookSecret.isBlank() && !webhookSecret.equals(secret)) {
            throw new RuntimeException("Μη εξουσιοδοτημένο webhook request");
        }

        // Αν υπάρχει ήδη το ΑΦΜ, ενημέρωσε μόνο
        if (customerRepository.existsByAfm(request.getAfm())) {
            log.info("Webhook: πελάτης με ΑΦΜ {} υπάρχει ήδη", request.getAfm());
            return;
        }

        // Βρες dealer/subdealer από referral code
        Dealer dealer       = null;
        SubDealer subDealer = null;

        if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
            var referral = referralCodeRepository
                    .findByCodeAndActiveTrue(request.getReferralCode())
                    .orElse(null);

            if (referral != null) {
                switch (referral.getEntityType().name()) {
                    case "DEALER" -> dealer = dealerRepository
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
            log.warn("Webhook: δεν βρέθηκε dealer για referral code: {}", request.getReferralCode());
            return;
        }

        Customer customer = Customer.builder()
                .id(idGenerator.generateCustomerId())
                .afm(request.getAfm())
                .eponymia(request.getEponymia())
                .nomimosEkprosopos(request.getNomimosEkprosopos())
                .epaggelma(request.getEpaggelma() != null ? request.getEpaggelma() : "—")
                .doy(request.getDoy() != null ? request.getDoy() : "—")
                .address(request.getAddress() != null ? request.getAddress() : "—")
                .city(request.getCity() != null ? request.getCity() : "—")
                .tk(request.getTk() != null ? request.getTk() : "00000")
                .phoneFixed(request.getPhoneFixed())
                .phoneMobile(request.getPhoneMobile())
                .email(request.getEmail())
                .active(true)
                .dealer(dealer)
                .subDealer(subDealer)
                .network(dealer.getNetwork())
                .source("API")
                .referralCode(request.getReferralCode())
                .build();

        customerRepository.save(customer);
        log.info("Webhook: νέος πελάτης {} καταχωρήθηκε επιτυχώς", request.getAfm());
    }
}
