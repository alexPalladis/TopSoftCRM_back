package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.response.ProductResponse;
import com.topsoft.topsoftcrm_backend.exception.ResourceNotFoundException;
import com.topsoft.topsoftcrm_backend.model.Product;
import com.topsoft.topsoftcrm_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream()
                .map(this::toResponse).toList();
    }

    public List<ProductResponse> getActive() {
        return productRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public ProductResponse toggleActive(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Προϊόν δεν βρέθηκε: " + id));
        product.setActive(!product.getActive());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updatePrice(Integer id, BigDecimal newPrice) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Προϊόν δεν βρέθηκε: " + id));
        product.setPrice(newPrice);
        return toResponse(productRepository.save(product));
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .description(p.getDescription())
                .price(p.getPrice())
                .type(p.getType())
                .active(p.getActive())
                .sortOrder(p.getSortOrder())
                .build();
    }
}