package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.NetworkRequest;
import com.topsoft.topsoftcrm_backend.dto.response.NetworkResponse;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.exception.ResourceNotFoundException;
import com.topsoft.topsoftcrm_backend.model.Network;
import com.topsoft.topsoftcrm_backend.repository.CustomerRepository;
import com.topsoft.topsoftcrm_backend.repository.DealerRepository;
import com.topsoft.topsoftcrm_backend.repository.NetworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NetworkService {

    private final NetworkRepository networkRepository;
    private final DealerRepository dealerRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder    passwordEncoder;
    private final IdGeneratorService idGenerator;

    public PageResponse<NetworkResponse> getAll(
            String city, Boolean active, String search,
            int page, int size) {

        var pageable = PageRequest.of(page, size, Sort.by("eponymia").ascending());
        Page<Network> result = networkRepository.findWithFilters(city, active, search, pageable);

        return PageResponse.<NetworkResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    public NetworkResponse getById(String id) {
        Network network = networkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Network δεν βρέθηκε: " + id));
        return toResponse(network);
    }

    @Transactional
    public NetworkResponse create(NetworkRequest request) {
        if (networkRepository.existsByAfm(request.getAfm()))
            throw new RuntimeException("ΑΦΜ υπάρχει ήδη");
        if (networkRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("Username υπάρχει ήδη");

        Network network = Network.builder()
                .id(idGenerator.generateNetworkId())
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
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return toResponse(networkRepository.save(network));
    }

    @Transactional
    public NetworkResponse update(String id, NetworkRequest request) {
        Network network = networkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Network δεν βρέθηκε: " + id));

        network.setEponymia(request.getEponymia());
        network.setNomimosEkprosopos(request.getNomimosEkprosopos());
        network.setEpaggelma(request.getEpaggelma());
        network.setDoy(request.getDoy());
        network.setAddress(request.getAddress());
        network.setCity(request.getCity());
        network.setTk(request.getTk());
        network.setPhoneFixed(request.getPhoneFixed());
        network.setPhoneMobile(request.getPhoneMobile());
        network.setEmail(request.getEmail());
        if (request.getActive() != null) network.setActive(request.getActive());
        if (request.getPassword() != null && !request.getPassword().isBlank())
            network.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        return toResponse(networkRepository.save(network));
    }

    @Transactional
    public void delete(String id) {
        Network network = networkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Network δεν βρέθηκε: " + id));

        long dealerCount = networkRepository.countDealersByNetworkId(id);
        if (dealerCount > 0)
            throw new RuntimeException("Δεν μπορεί να διαγραφεί — έχει " + dealerCount + " dealers");

        networkRepository.delete(network);
    }

    private NetworkResponse toResponse(Network n) {
        long totalDealers   = dealerRepository.findByNetworkId(n.getId()).size();
        long totalCustomers = customerRepository.countByNetworkId(n.getId());

        return NetworkResponse.builder()
                .id(n.getId())
                .afm(n.getAfm())
                .eponymia(n.getEponymia())
                .nomimosEkprosopos(n.getNomimosEkprosopos())
                .epaggelma(n.getEpaggelma())
                .doy(n.getDoy())
                .address(n.getAddress())
                .city(n.getCity())
                .tk(n.getTk())
                .phoneFixed(n.getPhoneFixed())
                .phoneMobile(n.getPhoneMobile())
                .email(n.getEmail())
                .username(n.getUsername())
                .active(n.getActive())
                .totalDealers(totalDealers)
                .totalCustomers(totalCustomers)
                .createdAt(n.getCreatedAt())
                .build();
    }
}
