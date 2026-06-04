package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.repository.CustomerRepository;
import com.topsoft.topsoftcrm_backend.repository.DealerRepository;
import com.topsoft.topsoftcrm_backend.repository.NetworkRepository;
import com.topsoft.topsoftcrm_backend.repository.SubDealerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdGeneratorService {

    private final NetworkRepository networkRepository;
    private final DealerRepository dealerRepository;
    private final SubDealerRepository subDealerRepository;
    private final CustomerRepository customerRepository;

    public String generateNetworkId() {
        return generateId("1", networkRepository.count());
    }

    public String generateDealerId() {
        return generateId("2", dealerRepository.count());
    }

    public String generateSubDealerId() {
        return generateId("3", subDealerRepository.count());
    }

    public String generateCustomerId() {
        return generateId("9", customerRepository.count());
    }

    private String generateId(String prefix, long count) {
        long next = count + 1;
        return prefix + String.format("%07d", next);
    }
}
