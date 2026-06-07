package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.SubDealerRequest;
import com.topsoft.topsoftcrm_backend.dto.response.LookupResponse;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.dto.response.SubDealerResponse;
import com.topsoft.topsoftcrm_backend.exception.ResourceNotFoundException;
import com.topsoft.topsoftcrm_backend.model.Dealer;
import com.topsoft.topsoftcrm_backend.model.ReferralCode;
import com.topsoft.topsoftcrm_backend.model.SubDealer;
import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import com.topsoft.topsoftcrm_backend.repository.CustomerRepository;
import com.topsoft.topsoftcrm_backend.repository.DealerRepository;
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
public class SubDealerService {

    private final SubDealerRepository    subDealerRepository;
    private final DealerRepository       dealerRepository;
    private final CustomerRepository     customerRepository;
    private final ReferralCodeRepository referralCodeRepository;
    private final PasswordEncoder        passwordEncoder;
    private final IdGeneratorService     idGenerator;
    private final AuditLogService        auditLogService;         // ← NEW

    // ---------------------------------------------------------------------- LIST
    public PageResponse<SubDealerResponse> getAll(
            String city, String dealerId, String networkId,
            Boolean active, String search, int page, int size) {

        var pageable = PageRequest.of(page, size, Sort.by("eponymia").ascending());
        Page<SubDealer> result = subDealerRepository
                .findWithFilters(city, dealerId, networkId, active, search, pageable);

        return PageResponse.<SubDealerResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages()).last(result.isLast())
                .build();
    }

    // ---------------------------------------------------------------------- GET
    public SubDealerResponse getById(String id) {
        return toResponse(subDealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubDealer δεν βρέθηκε: " + id)));
    }

    // ------------------------------------------------------------------- CREATE
    @Transactional
    public SubDealerResponse create(SubDealerRequest request, CrmUserPrincipal actor) {
        if (subDealerRepository.existsByAfm(request.getAfm()))
            throw new RuntimeException("ΑΦΜ υπάρχει ήδη");

        Dealer dealer = dealerRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε"));

        SubDealer subDealer = SubDealer.builder()
                .id(idGenerator.generateSubDealerId())
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
                .dealer(dealer)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        subDealerRepository.save(subDealer);

        // Referral code
        String referralCode = generateSubDealerReferralCode(subDealer.getId());
        referralCodeRepository.save(ReferralCode.builder()
                .code(referralCode).entityType(EntityType.SUBDEALER)
                .entityId(subDealer.getId()).active(true).build());

        // ── Audit log ──────────────────────────────────────────────────────
        auditLogService.log(
                actor.getId(), actor.getRole(), actor.getUsername(),
                "SUBDEALER", subDealer.getId(), subDealer.getEponymia(),
                "CREATE", "Δημιουργία sub-dealer: " + subDealer.getEponymia()
                        + " (Dealer: " + dealer.getEponymia() + ")");

        return toResponse(subDealer);
    }

    // ------------------------------------------------------------------- UPDATE
    @Transactional
    public SubDealerResponse update(String id, SubDealerRequest request, CrmUserPrincipal actor) {
        SubDealer subDealer = subDealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubDealer δεν βρέθηκε: " + id));

        Dealer dealer = dealerRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε"));

        subDealer.setEponymia(request.getEponymia());
        subDealer.setNomimosEkprosopos(request.getNomimosEkprosopos());
        subDealer.setEpaggelma(request.getEpaggelma());
        subDealer.setDoy(request.getDoy());
        subDealer.setAddress(request.getAddress());
        subDealer.setCity(request.getCity());
        subDealer.setTk(request.getTk());
        subDealer.setPhoneFixed(request.getPhoneFixed());
        subDealer.setPhoneMobile(request.getPhoneMobile());
        subDealer.setEmail(request.getEmail());
        subDealer.setDealer(dealer);
        if (request.getActive() != null) subDealer.setActive(request.getActive());
        if (request.getPassword() != null && !request.getPassword().isBlank())
            subDealer.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        SubDealerResponse response = toResponse(subDealerRepository.save(subDealer));

        // ── Audit log ──────────────────────────────────────────────────────
        auditLogService.log(
                actor.getId(), actor.getRole(), actor.getUsername(),
                "SUBDEALER", subDealer.getId(), subDealer.getEponymia(),
                "UPDATE", "Ενημέρωση sub-dealer: " + subDealer.getEponymia());

        return response;
    }

    // ------------------------------------------------------------------- DELETE
    @Transactional
    public void delete(String id, CrmUserPrincipal actor) {
        SubDealer subDealer = subDealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubDealer δεν βρέθηκε: " + id));

        long customerCount = customerRepository.countBySubDealerId(id);
        if (customerCount > 0)
            throw new RuntimeException("Δεν μπορεί να διαγραφεί — έχει " + customerCount + " πελάτες");

        String eponymia = subDealer.getEponymia();

        referralCodeRepository.findByEntityIdAndActiveTrue(id).ifPresent(referralCodeRepository::delete);

        // ── Audit log ΠΡΙΝ delete ──────────────────────────────────────────
        auditLogService.log(
                actor.getId(), actor.getRole(), actor.getUsername(),
                "SUBDEALER", id, eponymia,
                "DELETE", "Διαγραφή sub-dealer: " + eponymia);

        subDealerRepository.delete(subDealer);
    }

    // ----------------------------------------------------------------- MAPPING
    private SubDealerResponse toResponse(SubDealer s) {
        var network = s.getNetwork();
        return SubDealerResponse.builder()
                .id(s.getId()).afm(s.getAfm()).eponymia(s.getEponymia())
                .nomimosEkprosopos(s.getNomimosEkprosopos()).epaggelma(s.getEpaggelma())
                .doy(s.getDoy()).address(s.getAddress()).city(s.getCity()).tk(s.getTk())
                .phoneFixed(s.getPhoneFixed()).phoneMobile(s.getPhoneMobile())
                .email(s.getEmail()).username(s.getUsername()).active(s.getActive())
                .dealerId(s.getDealer().getId()).dealerName(s.getDealer().getEponymia())
                .networkId(network != null ? network.getId()         : null)
                .networkName(network != null ? network.getEponymia() : null)
                .totalCustomers(customerRepository.countBySubDealerId(s.getId()))
                .createdAt(s.getCreatedAt())
                .build();
    }

    private String generateSubDealerReferralCode(String subDealerId) {
        String suffix    = subDealerId.substring(Math.max(0, subDealerId.length() - 7));
        String candidate = "S" + suffix;
        if (!referralCodeRepository.existsById(candidate)) return candidate;
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    public List<LookupResponse> getLookup(CrmUserPrincipal principal) {
        return switch (principal.getRole()) {
            case "ADMIN" -> subDealerRepository.findAllByActiveTrueOrderByEponymiaAsc()
                    .stream()
                    .map(s -> new LookupResponse(s.getId(), s.getEponymia()))
                    .toList();

            case "NETWORK" -> {
                // Βλέπει subdealers του δικτύου του
                yield subDealerRepository.findActiveByNetworkId(principal.getId())
                        .stream()
                        .map(s -> new LookupResponse(s.getId(), s.getEponymia()))
                        .toList();
            }

            case "DEALER" -> {
                // Βλέπει μόνο τους subdealers του
                yield subDealerRepository.findByDealerIdAndActiveTrueOrderByEponymiaAsc(principal.getId())
                        .stream()
                        .map(s -> new LookupResponse(s.getId(), s.getEponymia()))
                        .toList();
            }

            case "SUBDEALER" -> {
                // Βλέπει μόνο τον εαυτό του
                SubDealer self = subDealerRepository.findById(principal.getId()).orElse(null);
                yield self != null
                        ? List.of(new LookupResponse(self.getId(), self.getEponymia()))
                        : List.of();
            }

            default -> List.of();
        };
    }
}