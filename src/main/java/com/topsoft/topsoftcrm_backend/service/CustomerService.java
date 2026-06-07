package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.CustomerReassignRequest;
import com.topsoft.topsoftcrm_backend.dto.request.CustomerRequest;
import com.topsoft.topsoftcrm_backend.dto.response.CustomerResponse;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.exception.ResourceNotFoundException;
import com.topsoft.topsoftcrm_backend.model.Customer;
import com.topsoft.topsoftcrm_backend.model.Dealer;
import com.topsoft.topsoftcrm_backend.model.SubDealer;
import com.topsoft.topsoftcrm_backend.repository.CustomerRepository;
import com.topsoft.topsoftcrm_backend.repository.DealerRepository;
import com.topsoft.topsoftcrm_backend.repository.SubDealerRepository;
import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository  customerRepository;
    private final DealerRepository    dealerRepository;
    private final SubDealerRepository subDealerRepository;
    private final IdGeneratorService  idGenerator;
    private final AuditLogService     auditLogService;          // ← NEW

    // ------------------------------------------------------------------- LIST
    public PageResponse<CustomerResponse> getAll(
            CrmUserPrincipal principal,
            String city, String dealerId, String networkId,
            Boolean active, String search, int page, int size) {

        var pageable = PageRequest.of(page, size, Sort.by("eponymia").ascending());

        Page<Customer> result = switch (principal.getRole()) {
            case "SUBDEALER" -> customerRepository.findBySubDealerId(
                    principal.getId(), city, active, search, pageable);
            case "DEALER" -> customerRepository.findWithFilters(
                    city, principal.getId(), null, active, search, pageable);
            case "NETWORK" -> customerRepository.findWithFilters(
                    city, null, principal.getId(), active, search, pageable);
            default -> customerRepository.findWithFilters(
                    city, dealerId, networkId, active, search, pageable);
        };

        return PageResponse.<CustomerResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages()).last(result.isLast())
                .build();
    }

    // ------------------------------------------------------------------- GET
    public CustomerResponse getById(String id) {
        return toResponse(customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Πελάτης δεν βρέθηκε: " + id)));
    }

    // ------------------------------------------------------------------- CREATE
    // Called by admin — no principal in this path; actor = ADMIN from SecurityContext
    @Transactional
    public CustomerResponse create(CustomerRequest request, CrmUserPrincipal actor) {
        if (customerRepository.existsByAfm(request.getAfm()))
            throw new RuntimeException("ΑΦΜ υπάρχει ήδη");

        Dealer dealer = dealerRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε"));

        SubDealer subDealer = null;
        if (request.getSubDealerId() != null && !request.getSubDealerId().isBlank()) {
            subDealer = subDealerRepository.findById(request.getSubDealerId())
                    .orElseThrow(() -> new ResourceNotFoundException("SubDealer δεν βρέθηκε"));
        }

        Customer customer = Customer.builder()
                .id(idGenerator.generateCustomerId())
                .afm(request.getAfm()).eponymia(request.getEponymia())
                .nomimosEkprosopos(request.getNomimosEkprosopos())
                .epaggelma(request.getEpaggelma()).doy(request.getDoy())
                .address(request.getAddress()).city(request.getCity()).tk(request.getTk())
                .phoneFixed(request.getPhoneFixed()).phoneMobile(request.getPhoneMobile())
                .email(request.getEmail())
                .active(request.getActive() != null ? request.getActive() : true)
                .dealer(dealer).subDealer(subDealer)
                .source("MANUAL").referralCode(request.getReferralCode())
                .build();

        CustomerResponse response = toResponse(customerRepository.save(customer));

        // ── Audit log ──────────────────────────────────────────────────────
        auditLogService.log(
                actor.getId(), actor.getRole(), actor.getUsername(),
                "CUSTOMER", customer.getId(), customer.getEponymia() + " (ΑΦΜ: " + customer.getAfm() + ")",
                "CREATE", "Χειροκίνητη δημιουργία πελάτη: " + customer.getEponymia());

        return response;
    }

    // ------------------------------------------------------------------- UPDATE
    @Transactional
    public CustomerResponse update(String id, CustomerRequest request, CrmUserPrincipal actor) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Πελάτης δεν βρέθηκε: " + id));

        Dealer dealer = dealerRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε"));

        SubDealer subDealer = null;
        if (request.getSubDealerId() != null && !request.getSubDealerId().isBlank()) {
            subDealer = subDealerRepository.findById(request.getSubDealerId())
                    .orElseThrow(() -> new ResourceNotFoundException("SubDealer δεν βρέθηκε"));
        }

        customer.setAfm(request.getAfm());
        customer.setEponymia(request.getEponymia());
        customer.setNomimosEkprosopos(request.getNomimosEkprosopos());
        customer.setEpaggelma(request.getEpaggelma());
        customer.setDoy(request.getDoy());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setTk(request.getTk());
        customer.setPhoneFixed(request.getPhoneFixed());
        customer.setPhoneMobile(request.getPhoneMobile());
        customer.setEmail(request.getEmail());
        customer.setDealer(dealer);
        customer.setSubDealer(subDealer);
        if (request.getActive() != null) customer.setActive(request.getActive());

        CustomerResponse response = toResponse(customerRepository.save(customer));

        // ── Audit log ──────────────────────────────────────────────────────
        auditLogService.log(
                actor.getId(), actor.getRole(), actor.getUsername(),
                "CUSTOMER", customer.getId(), customer.getEponymia() + " (ΑΦΜ: " + customer.getAfm() + ")",
                "UPDATE", "Ενημέρωση πελάτη: " + customer.getEponymia());

        return response;
    }

    // ------------------------------------------------------------------- DELETE
    @Transactional
    public void delete(String id, CrmUserPrincipal actor) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Πελάτης δεν βρέθηκε: " + id));

        String label = customer.getEponymia() + " (ΑΦΜ: " + customer.getAfm() + ")";

        // ── Audit log ΠΡΙΝ delete ──────────────────────────────────────────
        auditLogService.log(
                actor.getId(), actor.getRole(), actor.getUsername(),
                "CUSTOMER", id, label,
                "DELETE", "Διαγραφή πελάτη: " + label);

        customerRepository.delete(customer);
    }

    // ----------------------------------------------------------------- REASSIGN
    @Transactional
    public CustomerResponse reassign(
            String customerId, CrmUserPrincipal principal, CustomerReassignRequest request) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Πελάτης δεν βρέθηκε: " + customerId));

        String oldSubDealer = customer.getSubDealer() != null
                ? customer.getSubDealer().getEponymia() : "—";

        String role = principal.getRole();

        if ("DEALER".equals(role)) {
            if (!customer.getDealer().getId().equals(principal.getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Δεν έχετε δικαίωμα να αλλάξετε αυτόν τον πελάτη");
            if (request.getSubDealerId() == null || request.getSubDealerId().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ο dealer πρέπει να επιλέξει έναν sub-dealer");

            SubDealer subDealer = subDealerRepository.findById(request.getSubDealerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sub-dealer δεν βρέθηκε"));
            if (!subDealer.getDealer().getId().equals(principal.getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ο sub-dealer δεν ανήκει στο δίκτυό σας");

            customer.setSubDealer(subDealer);

            auditLogService.log(
                    principal.getId(), principal.getRole(), principal.getUsername(),
                    "CUSTOMER", customer.getId(), customer.getEponymia(),
                    "REASSIGN", "Μεταφορά από sub-dealer [" + oldSubDealer + "] → [" + subDealer.getEponymia() + "]");

        } else if ("ADMIN".equals(role)) {
            if (request.getSubDealerId() == null || request.getSubDealerId().isBlank()) {
                customer.setSubDealer(null);
                auditLogService.log(
                        principal.getId(), principal.getRole(), principal.getUsername(),
                        "CUSTOMER", customer.getId(), customer.getEponymia(),
                        "REASSIGN", "Αποσύνδεση από sub-dealer [" + oldSubDealer + "]");
            } else {
                SubDealer subDealer = subDealerRepository.findById(request.getSubDealerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Sub-dealer δεν βρέθηκε"));
                customer.setSubDealer(subDealer);
                auditLogService.log(
                        principal.getId(), principal.getRole(), principal.getUsername(),
                        "CUSTOMER", customer.getId(), customer.getEponymia(),
                        "REASSIGN", "Μεταφορά από sub-dealer [" + oldSubDealer + "] → [" + subDealer.getEponymia() + "]");
            }
        }

        return toResponse(customerRepository.save(customer));
    }

    // ----------------------------------------------------------------- MAPPING
    private CustomerResponse toResponse(Customer c) {
        var network = c.getNetwork();
        return CustomerResponse.builder()
                .id(c.getId()).afm(c.getAfm()).eponymia(c.getEponymia())
                .nomimosEkprosopos(c.getNomimosEkprosopos()).epaggelma(c.getEpaggelma())
                .doy(c.getDoy()).address(c.getAddress()).city(c.getCity()).tk(c.getTk())
                .phoneFixed(c.getPhoneFixed()).phoneMobile(c.getPhoneMobile())
                .email(c.getEmail()).active(c.getActive())
                .dealerId(c.getDealer().getId()).dealerName(c.getDealer().getEponymia())
                .subDealerId(c.getSubDealer()  != null ? c.getSubDealer().getId()         : null)
                .subDealerName(c.getSubDealer() != null ? c.getSubDealer().getEponymia()  : null)
                .networkId(network   != null ? network.getId()         : null)
                .networkName(network != null ? network.getEponymia()   : null)
                .source(c.getSource()).referralCode(c.getReferralCode())
                .createdAt(c.getCreatedAt())
                .build();
    }
}