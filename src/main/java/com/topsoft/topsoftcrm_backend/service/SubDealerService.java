package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.SubDealerRequest;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.dto.response.SubDealerResponse;
import com.topsoft.topsoftcrm_backend.exception.ResourceNotFoundException;
import com.topsoft.topsoftcrm_backend.model.Dealer;
import com.topsoft.topsoftcrm_backend.model.SubDealer;
import com.topsoft.topsoftcrm_backend.repository.CustomerRepository;
import com.topsoft.topsoftcrm_backend.repository.DealerRepository;
import com.topsoft.topsoftcrm_backend.repository.SubDealerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubDealerService {

    private final SubDealerRepository subDealerRepository;
    private final DealerRepository    dealerRepository;
    private final CustomerRepository  customerRepository;
    private final PasswordEncoder     passwordEncoder;
    private final IdGeneratorService  idGenerator;

    // ---------------------------------------------------------------------- LIST
    public PageResponse<SubDealerResponse> getAll(
            String city, String dealerId, String networkId,
            Boolean active, String search, int page, int size) {

        var pageable = PageRequest.of(page, size, Sort.by("eponymia").ascending());
        Page<SubDealer> result = subDealerRepository
                .findWithFilters(city, dealerId, networkId, active, search, pageable);

        return PageResponse.<SubDealerResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    // ---------------------------------------------------------------------- GET
    public SubDealerResponse getById(String id) {
        return toResponse(subDealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubDealer δεν βρέθηκε: " + id)));
    }

    // ------------------------------------------------------------------- CREATE
    @Transactional
    public SubDealerResponse create(SubDealerRequest request) {
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
                // network is NOT stored — derived via dealer.getNetwork() at read time
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return toResponse(subDealerRepository.save(subDealer));
    }

    // ------------------------------------------------------------------- UPDATE
    @Transactional
    public SubDealerResponse update(String id, SubDealerRequest request) {
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
        // network is NOT stored — automatically correct because dealer carries it
        if (request.getActive() != null) subDealer.setActive(request.getActive());
        if (request.getPassword() != null && !request.getPassword().isBlank())
            subDealer.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        return toResponse(subDealerRepository.save(subDealer));
    }

    // ------------------------------------------------------------------- DELETE
    @Transactional
    public void delete(String id) {
        SubDealer subDealer = subDealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubDealer δεν βρέθηκε: " + id));

        long customerCount = customerRepository.countBySubDealerId(id);
        if (customerCount > 0)
            throw new RuntimeException(
                    "Δεν μπορεί να διαγραφεί — έχει " + customerCount + " πελάτες");

        subDealerRepository.delete(subDealer);
    }

    // ----------------------------------------------------------------- MAPPING
    private SubDealerResponse toResponse(SubDealer s) {
        // getNetwork() is the convenience method on SubDealer — derives via dealer.
        // dealer is already JOIN FETCHed in the repository query, so no extra query here.
        var network = s.getNetwork();

        return SubDealerResponse.builder()
                .id(s.getId())
                .afm(s.getAfm())
                .eponymia(s.getEponymia())
                .nomimosEkprosopos(s.getNomimosEkprosopos())
                .epaggelma(s.getEpaggelma())
                .doy(s.getDoy())
                .address(s.getAddress())
                .city(s.getCity())
                .tk(s.getTk())
                .phoneFixed(s.getPhoneFixed())
                .phoneMobile(s.getPhoneMobile())
                .email(s.getEmail())
                .username(s.getUsername())
                .active(s.getActive())
                .dealerId(s.getDealer().getId())
                .dealerName(s.getDealer().getEponymia())
                .networkId(network != null ? network.getId()       : null)
                .networkName(network != null ? network.getEponymia() : null)
                .totalCustomers(customerRepository.countBySubDealerId(s.getId()))
                .createdAt(s.getCreatedAt())
                .build();
    }
}