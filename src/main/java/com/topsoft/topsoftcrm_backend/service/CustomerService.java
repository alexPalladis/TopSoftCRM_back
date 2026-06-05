package com.topsoft.topsoftcrm_backend.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final DealerRepository dealerRepository;
    private final SubDealerRepository subDealerRepository;
    private final IdGeneratorService  idGenerator;

    public PageResponse<CustomerResponse> getAll(
            CrmUserPrincipal principal,
            String city, String dealerId, String networkId,
            Boolean active, String search, int page, int size) {

        // Φιλτράρισμα ανά ρόλο
        String effectiveNetworkId = networkId;
        String effectiveDealerId  = dealerId;

        switch (principal.getRole()) {
            case "NETWORK"   -> effectiveNetworkId = principal.getId();
            case "DEALER"    -> effectiveDealerId  = principal.getId();
            case "SUBDEALER" -> {
                // SubDealer βλέπει μόνο πελάτες του
                var sub = subDealerRepository.findById(principal.getId()).orElse(null);
                if (sub != null) effectiveDealerId = sub.getDealer().getId();
            }
        }

        var pageable = PageRequest.of(page, size, Sort.by("eponymia").ascending());
        Page<Customer> result = customerRepository.findWithFilters(
                city, effectiveDealerId, effectiveNetworkId, active, search, pageable);

        return PageResponse.<CustomerResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    public CustomerResponse getById(String id) {
        return toResponse(customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Πελάτης δεν βρέθηκε: " + id)));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
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
                .active(request.getActive() != null ? request.getActive() : true)
                .dealer(dealer)
                .subDealer(subDealer)
                .network(dealer.getNetwork())
                .source("MANUAL")
                .referralCode(request.getReferralCode())
                .build();

        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(String id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Πελάτης δεν βρέθηκε: " + id));

        Dealer dealer = dealerRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε"));

        SubDealer subDealer = null;
        if (request.getSubDealerId() != null && !request.getSubDealerId().isBlank()) {
            subDealer = subDealerRepository.findById(request.getSubDealerId())
                    .orElseThrow(() -> new ResourceNotFoundException("SubDealer δεν βρέθηκε"));
        }

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
        customer.setNetwork(dealer.getNetwork());
        if (request.getActive() != null) customer.setActive(request.getActive());

        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Πελάτης δεν βρέθηκε: " + id));
        customerRepository.delete(customer);
    }

    private CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .afm(c.getAfm())
                .eponymia(c.getEponymia())
                .nomimosEkprosopos(c.getNomimosEkprosopos())
                .epaggelma(c.getEpaggelma())
                .doy(c.getDoy())
                .address(c.getAddress())
                .city(c.getCity())
                .tk(c.getTk())
                .phoneFixed(c.getPhoneFixed())
                .phoneMobile(c.getPhoneMobile())
                .email(c.getEmail())
                .active(c.getActive())
                .dealerId(c.getDealer().getId())
                .dealerName(c.getDealer().getEponymia())
                .subDealerId(c.getSubDealer() != null ? c.getSubDealer().getId() : null)
                .subDealerName(c.getSubDealer() != null ? c.getSubDealer().getEponymia() : null)
                .networkId(c.getNetwork() != null ? c.getNetwork().getId() : null)
                .networkName(c.getNetwork() != null ? c.getNetwork().getEponymia() : null)
                .source(c.getSource())
                .referralCode(c.getReferralCode())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
