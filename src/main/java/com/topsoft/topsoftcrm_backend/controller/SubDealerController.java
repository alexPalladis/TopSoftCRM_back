package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.request.SubDealerRequest;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.dto.response.SubDealerResponse;
import com.topsoft.topsoftcrm_backend.service.SubDealerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subdealers")
@RequiredArgsConstructor
public class SubDealerController {

    private final SubDealerService subDealerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER', 'SUBDEALER')")
    public ResponseEntity<PageResponse<SubDealerResponse>> getAll(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String dealerId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                subDealerService.getAll(city, dealerId, active, search, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK', 'DEALER', 'SUBDEALER')")
    public ResponseEntity<SubDealerResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(subDealerService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    public ResponseEntity<SubDealerResponse> create(
            @Valid @RequestBody SubDealerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subDealerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    public ResponseEntity<SubDealerResponse> update(
            @PathVariable String id,
            @Valid @RequestBody SubDealerRequest request) {
        return ResponseEntity.ok(subDealerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEALER')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        subDealerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
