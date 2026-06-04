package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.DealerRequest;
import com.topsoft.topsoftcrm_backend.dto.response.DealerResponse;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.exception.ResourceNotFoundException;
import com.topsoft.topsoftcrm_backend.model.Dealer;
import com.topsoft.topsoftcrm_backend.model.Network;
import com.topsoft.topsoftcrm_backend.repository.CustomerRepository;
import com.topsoft.topsoftcrm_backend.repository.DealerRepository;
import com.topsoft.topsoftcrm_backend.repository.NetworkRepository;
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
public class DealerService {

    private final DealerRepository dealerRepository;
    private final NetworkRepository networkRepository;
    private final SubDealerRepository subDealerRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder       passwordEncoder;
    private final IdGeneratorService    idGenerator;

    public PageResponse<DealerResponse> getAll(
            String city, String networkId, Boolean active, String search,
            int page, int size) {

        var pageable = PageRequest.of(page, size, Sort.by("eponymia").ascending());
        Page<Dealer> result = dealerRepository.findWithFilters(city, networkId, active, search, pageable);

        return PageResponse.<DealerResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    public DealerResponse getById(String id) {
        return toResponse(dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε: " + id)));
    }

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

        return toResponse(dealerRepository.save(dealer));
    }

    @Transactional
    public DealerResponse update(String id, DealerRequest request) {
        Dealer dealer = dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε: " + id));

        Network network = null;
        if (request.getNetworkId() != null && !request.getNetworkId().isBlank()) {
            network = networkRepository.findById(request.getNetworkId())
                    .orElseThrow(() -> new ResourceNotFoundException("Network δεν βρέθηκε"));
        }

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

    @Transactional
    public void delete(String id) {
        Dealer dealer = dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε: " + id));

        long customerCount = customerRepository.countByDealerId(id);
        if (customerCount > 0)
            throw new RuntimeException("Δεν μπορεί να διαγραφεί — έχει " + customerCount + " πελάτες");

        dealerRepository.delete(dealer);
    }

    private DealerResponse toResponse(Dealer d) {
        long totalSubDealers = subDealerRepository.countByDealerId(d.getId());
        long totalCustomers  = customerRepository.countByDealerId(d.getId());

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
                .networkId(d.getNetwork() != null ? d.getNetwork().getId() : null)
                .networkName(d.getNetwork() != null ? d.getNetwork().getEponymia() : null)
                .totalSubDealers(totalSubDealers)
                .totalCustomers(totalCustomers)
                .createdAt(d.getCreatedAt())
                .build();
    }
}