package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.DealerRequest;
import com.topsoft.topsoftcrm_backend.dto.response.DealerResponse;
import com.topsoft.topsoftcrm_backend.dto.response.LookupResponse;
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

import java.util.Comparator;
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
    private final ReferralCodeRepository referralCodeRepository;
    private final PasswordEncoder        passwordEncoder;
    private final IdGeneratorService     idGenerator;
    private final AuditLogService        auditLogService;         // ← NEW

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
                        .page(0).size(1).totalElements(1).totalPages(1).last(true).build();
            }
        }

        var pageable = PageRequest.of(page, size, Sort.by("eponymia").ascending());
        Page<Dealer> result = dealerRepository.findWithFilters(city, effectiveNetworkId, active, search, pageable);

        return PageResponse.<DealerResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages()).last(result.isLast())
                .build();
    }

    // -------------------------------------------------------------------- GET
    public DealerResponse getById(String id) {
        return toResponse(dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε: " + id)));
    }

    // ----------------------------------------------------------------- CREATE
    @Transactional
    public DealerResponse create(DealerRequest request, CrmUserPrincipal actor) {
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

        // Pre-fill commissions from global Dealer defaults
        List<Commission> defaults = commissionRepository
                .findByEntityTypeAndEntityId(EntityType.DEALER, DEALER_DEFAULT_ID);
        for (Commission def : defaults) {
            commissionRepository.save(Commission.builder()
                    .entityType(EntityType.DEALER).entityId(dealer.getId())
                    .productId(def.getProductId())
                    .percentage(def.getPercentage()).salePrice(def.getSalePrice())
                    .build());
        }

        // Referral code
        String referralCode = generateDealerReferralCode(dealer.getId());
        referralCodeRepository.save(ReferralCode.builder()
                .code(referralCode).entityType(EntityType.DEALER)
                .entityId(dealer.getId()).active(true).build());

        // ── Audit log ──────────────────────────────────────────────────────
        auditLogService.log(
                actor.getId(), actor.getRole(), actor.getUsername(),
                "DEALER", dealer.getId(), dealer.getEponymia(),
                "CREATE", "Δημιουργία dealer: " + dealer.getEponymia()
                        + (network != null ? " (Network: " + network.getEponymia() + ")" : ""));

        return toResponse(dealer);
    }

    // ----------------------------------------------------------------- UPDATE
    @Transactional
    public DealerResponse update(String id, DealerRequest request, CrmUserPrincipal actor) {
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

        DealerResponse response = toResponse(dealerRepository.save(dealer));

        // ── Audit log ──────────────────────────────────────────────────────
        auditLogService.log(
                actor.getId(), actor.getRole(), actor.getUsername(),
                "DEALER", dealer.getId(), dealer.getEponymia(),
                "UPDATE", "Ενημέρωση dealer: " + dealer.getEponymia());

        return response;
    }

    // ----------------------------------------------------------------- DELETE
    @Transactional
    public void delete(String id, CrmUserPrincipal actor) {
        Dealer dealer = dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε: " + id));

        long subCount      = subDealerRepository.countByDealerId(id);
        long customerCount = customerRepository.countByDealerId(id);

        if (subCount > 0)
            throw new RuntimeException("Δεν μπορεί να διαγραφεί — έχει " + subCount + " sub-dealers");
        if (customerCount > 0)
            throw new RuntimeException("Δεν μπορεί να διαγραφεί — έχει " + customerCount + " πελάτες");

        String eponymia = dealer.getEponymia();

        referralCodeRepository.findByEntityIdAndActiveTrue(id).ifPresent(referralCodeRepository::delete);

        // ── Audit log ΠΡΙΝ delete (μετά χάνεται το entity) ────────────────
        auditLogService.log(
                actor.getId(), actor.getRole(), actor.getUsername(),
                "DEALER", id, eponymia,
                "DELETE", "Διαγραφή dealer: " + eponymia);

        dealerRepository.delete(dealer);
    }

    // ----------------------------------------------------------------- MAPPING
    private DealerResponse toResponse(Dealer d) {
        var network = d.getNetwork();
        return DealerResponse.builder()
                .id(d.getId()).afm(d.getAfm()).eponymia(d.getEponymia())
                .nomimosEkprosopos(d.getNomimosEkprosopos()).epaggelma(d.getEpaggelma())
                .doy(d.getDoy()).address(d.getAddress()).city(d.getCity()).tk(d.getTk())
                .phoneFixed(d.getPhoneFixed()).phoneMobile(d.getPhoneMobile())
                .email(d.getEmail()).username(d.getUsername()).active(d.getActive())
                .networkId(network != null ? network.getId()         : null)
                .networkName(network != null ? network.getEponymia() : null)
                .totalSubDealers(subDealerRepository.countByDealerId(d.getId()))
                .createdAt(d.getCreatedAt())
                .build();
    }

    private String generateDealerReferralCode(String dealerId) {
        String suffix    = dealerId.substring(Math.max(0, dealerId.length() - 7));
        String candidate = "D" + suffix;
        if (!referralCodeRepository.existsById(candidate)) return candidate;
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    public List<LookupResponse> getLookup(CrmUserPrincipal principal) {
        return switch (principal.getRole()) {
            case "ADMIN" -> dealerRepository.findAllByActiveTrueOrderByEponymiaAsc()
                    .stream()
                    .map(d -> new LookupResponse(d.getId(), d.getEponymia()))
                    .toList();

            case "NETWORK" -> {
                // Βλέπει μόνο dealers του δικτύου του
                yield dealerRepository.findByNetworkId(principal.getId())
                        .stream()
                        .filter(d -> Boolean.TRUE.equals(d.getActive()))
                        .sorted(Comparator.comparing(Dealer::getEponymia))
                        .map(d -> new LookupResponse(d.getId(), d.getEponymia()))
                        .toList();
            }

            case "DEALER" -> {
                // Βλέπει μόνο τον εαυτό του
                Dealer self = dealerRepository.findById(principal.getId()).orElse(null);
                yield self != null
                        ? List.of(new LookupResponse(self.getId(), self.getEponymia()))
                        : List.of();
            }

            case "SUBDEALER" -> {
                // Βλέπει μόνο τον dealer που ανήκει
                yield dealerRepository.findDealerBySubDealerId(principal.getId())
                        .map(d -> List.of(new LookupResponse(d.getId(), d.getEponymia())))
                        .orElse(List.of());
            }

            default -> List.of();
        };
    }
}