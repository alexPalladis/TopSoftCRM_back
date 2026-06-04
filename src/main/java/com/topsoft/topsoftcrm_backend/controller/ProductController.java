package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.response.ProductResponse;
import com.topsoft.topsoftcrm_backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER', 'SUBDEALER')")
    public ResponseEntity<List<ProductResponse>> getAll() {
        return ResponseEntity.ok(productService.getAll());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER', 'SUBDEALER')")
    public ResponseEntity<List<ProductResponse>> getActive() {
        return ResponseEntity.ok(productService.getActive());
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> toggleActive(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.toggleActive(id));
    }

    @PatchMapping("/{id}/price")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updatePrice(
            @PathVariable Integer id,
            @RequestBody Map<String, BigDecimal> body) {
        return ResponseEntity.ok(productService.updatePrice(id, body.get("price")));
    }
}
