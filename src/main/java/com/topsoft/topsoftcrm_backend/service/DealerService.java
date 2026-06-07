package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.DealerRequest;
import com.topsoft.topsoftcrm_backend.dto.response.DealerResponse;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.exception.ResourceNotFoundException;
import com.topsoft.topsoftcrm_backend.model.Commission;
import com.topsoft.topsoftcrm_backend.model.Dealer;
import com.topsoft.topsoftcrm_backend.model.Network;
import com.topsoft.topsoftcrm_backend.model.ReferralCode;
import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import com.topsoft.topsoftcrm_backend.repository.CommissionRepository;
import com.topsoft.topsoftcrm_backend.repository.CustomerRepository;
import com.topsoft.topsoftcrm_backend.repository.DealerRepository;
import com.topsoft.topsoftcrm_backend.repository.NetworkRepository;
import com.topsoft.topsoftcrm_backend.repository.ReferralCodeRepository;
import com.topsoft.topsoftcrm_backend.repository.SubDealerRepository;
import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DealerService {

    private final DealerRepository       dealerRepository;
    private final NetworkRepository      networkRepository;
    private final SubDealerRepository    subDealerRepository;
    private final CustomerRepository     customerRepository;
    private final CommissionRepository   commissionRepository;
    private final ReferralCodeRepository referralCodeRepository;  // ← νέο
    private final PasswordEncoder        passwordEncoder;
    private final IdGeneratorService     idGenerator;

    // Reserved sentinel entity_id for the global Dealer commission defaults.
    private static final String DEALER_DEFAULT_ID = "00000020";

    // ------------------------------------------------------------------- LIST
    public PageResponse<DealerResponse> getAll(
            CrmUserPrincipal principal,
            String city, String networkId, Boolean active, String search,
            int page, int size) {

        String effectiveNetworkId = networkId;

        switch (principal.getRole()) {
            case "NETWORK" -> effectiveNetworkId = principal.getId();
            case "DEALER"  -> {
                return PageResponse.<DealerResponse>builder()
                        .content(List.of(toResponse(dealerRepository.findById(principal.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε")))))
                        .page(0).size(1).totalElements(1).totalPages(1).last(true)
                        .build();
            }
        }

        var pageable = PageRequest.of(page, size, Sort.by("eponymia").ascending());
        Page<Dealer> result = dealerRepository.findWithFilters(city, effectiveNetworkId, active, search, pageable);

        return PageResponse.<DealerResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    // -------------------------------------------------------------------- GET
    public DealerResponse getById(String id) {
        return toResponse(dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε: " + id)));
    }

    // ----------------------------------------------------------------- CREATE
    @Transactional
    public DealerResponse create(DealerRequest request) {
        if (dealerRepository.existsByAfm(request.getAfm()))
            throw new RuntimeException("ΑΦΜ υπάρχει ήδη");

        Network network = null;
        if (request.getNetworkId() != null && !request.getNetworkId().isBlank()) {
            network = networkRepository.findById(request.getNetworkId())
                    .orElseThrow(() -> new ResourceNotFoundException("Network δεν βρέθηκε"));
        }

        Dealer dealer = Dealer.builder()
                .id(idGenerator.generateDealerId())
                .afm(request.getAfm())
                .eponymia(request.getEponymia())
                .nomimosEkprosopos(request.getNomimosEkprosopos())
                .epaggelma(request.getEpaggelma())
                .doy(request.getDoy())
                .address(request.getAddress())
                .city(request.getCity())
                .tk(request.getTk())
                .phoneFixed(request.getPhoneFixed())
                .phoneMobile(request.getPhoneMobile())
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .network(network)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        dealerRepository.save(dealer);

        // ── Pre-fill commissions from global Dealer defaults ──────────────────
        List<Commission> defaults = commissionRepository
                .findByEntityTypeAndEntityId(EntityType.DEALER, DEALER_DEFAULT_ID);
        for (Commission def : defaults) {
            Commission c = Commission.builder()
                    .entityType(EntityType.DEALER)
                    .entityId(dealer.getId())
                    .productId(def.getProductId())
                    .percentage(def.getPercentage())
                    .salePrice(def.getSalePrice())
                    .build();
            commissionRepository.save(c);
        }

        // ── Δημιουργία referral code για τον νέο Dealer ───────────────────────
        // Το code είναι μοναδικό 8ψήφιο string (π.χ. "D2000001").
        // Prefix "D" + τα τελευταία 7 ψηφία του dealer ID.
        String referralCode = generateDealerReferralCode(dealer.getId());
        referralCodeRepository.save(
                ReferralCode.builder()
                        .code(referralCode)
                        .entityType(EntityType.DEALER)
                        .entityId(dealer.getId())
                        .active(true)
                        .build()
        );

        return toResponse(dealer);
    }

    // ----------------------------------------------------------------- UPDATE
    @Transactional
    public DealerResponse update(String id, DealerRequest request) {
        Dealer dealer = dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε: " + id));

        Network network = null;
        if (request.getNetworkId() != null && !request.getNetworkId().isBlank()) {
            network = networkRepository.findById(request.getNetworkId())
                    .orElseThrow(() -> new ResourceNotFoundException("Network δεν βρέθηκε"));
        }

        dealer.setAfm(request.getAfm());
        dealer.setEponymia(request.getEponymia());
        dealer.setNomimosEkprosopos(request.getNomimosEkprosopos());
        dealer.setEpaggelma(request.getEpaggelma());
        dealer.setDoy(request.getDoy());
        dealer.setAddress(request.getAddress());
        dealer.setCity(request.getCity());
        dealer.setTk(request.getTk());
        dealer.setPhoneFixed(request.getPhoneFixed());
        dealer.setPhoneMobile(request.getPhoneMobile());
        dealer.setEmail(request.getEmail());
        dealer.setNetwork(network);
        if (request.getActive() != null) dealer.setActive(request.getActive());
        if (request.getPassword() != null && !request.getPassword().isBlank())
            dealer.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        return toResponse(dealerRepository.save(dealer));
    }

    // ----------------------------------------------------------------- DELETE
    @Transactional
    public void delete(String id) {
        Dealer dealer = dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε: " + id));

        long subCount      = subDealerRepository.countByDealerId(id);
        long customerCount = customerRepository.countByDealerId(id);

        if (subCount > 0)
            throw new RuntimeException("Δεν μπορεί να διαγραφεί — έχει " + subCount + " sub-dealers");
        if (customerCount > 0)
            throw new RuntimeException("Δεν μπορεί να διαγραφεί — έχει " + customerCount + " πελάτες");

        // Διαγραφή referral codes του dealer
        referralCodeRepository.findByEntityIdAndActiveTrue(id)
                .ifPresent(referralCodeRepository::delete);

        dealerRepository.delete(dealer);
    }

    // ----------------------------------------------------------------- MAPPING
    private DealerResponse toResponse(Dealer d) {
        var network = d.getNetwork();
        return DealerResponse.builder()
                .id(d.getId())
                .afm(d.getAfm())
                .eponymia(d.getEponymia())
                .nomimosEkprosopos(d.getNomimosEkprosopos())
                .epaggelma(d.getEpaggelma())
                .doy(d.getDoy())
                .address(d.getAddress())
                .city(d.getCity())
                .tk(d.getTk())
                .phoneFixed(d.getPhoneFixed())
                .phoneMobile(d.getPhoneMobile())
                .email(d.getEmail())
                .username(d.getUsername())
                .active(d.getActive())
                .networkId(network != null ? network.getId()         : null)
                .networkName(network != null ? network.getEponymia() : null)
                .totalSubDealers(subDealerRepository.countByDealerId(d.getId()))
                .createdAt(d.getCreatedAt())
                .build();
    }

    // ── Referral code generation ──────────────────────────────────────────────
    // Format: "D" + τα 7 τελευταία ψηφία του dealer ID  →  π.χ. "D2000001"
    // Αν υπάρχει σύγκρουση (εξαιρετικά σπάνιο), fallback σε UUID prefix.
    private String generateDealerReferralCode(String dealerId) {
        // dealerId is like "20000001" — take last 7 chars
        String suffix = dealerId.substring(Math.max(0, dealerId.length() - 7));
        String candidate = "D" + suffix;
        if (!referralCodeRepository.existsById(candidate)) {
            return candidate;
        }
        // Fallback: first 8 chars of UUID (collision-safe)
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}