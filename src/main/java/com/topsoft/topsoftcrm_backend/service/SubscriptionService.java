package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.SubscriptionRequest;
import com.topsoft.topsoftcrm_backend.dto.response.SubscriptionResponse;
import com.topsoft.topsoftcrm_backend.exception.ResourceNotFoundException;
import com.topsoft.topsoftcrm_backend.model.Customer;
import com.topsoft.topsoftcrm_backend.model.Product;
import com.topsoft.topsoftcrm_backend.model.Subscription;
import com.topsoft.topsoftcrm_backend.repository.CustomerRepository;
import com.topsoft.topsoftcrm_backend.repository.ProductRepository;
import com.topsoft.topsoftcrm_backend.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public List<SubscriptionResponse> getByCustomer(String customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Πελάτης δεν βρέθηκε"));

        var products = productRepository.findByActiveTrueOrderBySortOrderAsc();
        var existing = subscriptionRepository.findByCustomerId(customerId);

        return products.stream().map(p -> {
            var sub = existing.stream()
                    .filter(s -> s.getProduct().getId().equals(p.getId()))
                    .findFirst();
            return sub.map(this::toResponse).orElse(
                    SubscriptionResponse.builder()
                            .productId(p.getId())
                            .productDescription(p.getDescription())
                            .productType(p.getType())
                            .active(false)
                            .cost(p.getPrice())
                            .build()
            );
        }).toList();
    }

    @Transactional
    public SubscriptionResponse upsert(String customerId, SubscriptionRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Πελάτης δεν βρέθηκε"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Προϊόν δεν βρέθηκε"));

        var existing = subscriptionRepository
                .findByCustomerIdAndProductId(customerId, request.getProductId());

        Subscription sub;
        if (existing.isPresent()) {
            sub = existing.get();
        } else {
            sub = Subscription.builder()
                    .customer(customer)
                    .product(product)
                    .cost(product.getPrice())
                    .build();
        }

        sub.setActive(request.getActive());
        sub.setExpiryDate(request.getExpiryDate());
        sub.setQuantity(request.getQuantity());
        sub.setUpdatedAt(LocalDateTime.now());

        if (request.getCostOverride() != null)
            sub.setCost(request.getCostOverride());

        if (Boolean.TRUE.equals(request.getActive()) && sub.getActivatedAt() == null)
            sub.setActivatedAt(LocalDateTime.now());

        return toResponse(subscriptionRepository.save(sub));
    }

    private SubscriptionResponse toResponse(Subscription s) {
        return SubscriptionResponse.builder()
                .id(s.getId())
                .productId(s.getProduct().getId())
                .productDescription(s.getProduct().getDescription())
                .productType(s.getProduct().getType())
                .active(s.getActive())
                .expiryDate(s.getExpiryDate())
                .quantity(s.getQuantity())
                .cost(s.getCost())
                .activatedAt(s.getActivatedAt())
                .build();
    }
}
