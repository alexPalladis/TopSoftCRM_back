package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.request.TicketRequest;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.dto.response.TicketResponse;
import com.topsoft.topsoftcrm_backend.model.enums.TicketStatus;
import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import com.topsoft.topsoftcrm_backend.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * GET /api/tickets
     *
     * All three filter params are optional — omit them to get unfiltered results.
     *
     * @param status   PENDING | DONE  (optional)
     * @param dateFrom ISO date yyyy-MM-dd (optional)
     * @param dateTo   ISO date yyyy-MM-dd (optional)
     * @param page     0-based page number (default 0)
     * @param size     page size (default 10)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','NETWORK','DEALER','SUBDEALER')")
    public ResponseEntity<PageResponse<TicketResponse>> getAll(
            @AuthenticationPrincipal CrmUserPrincipal principal,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                ticketService.getAll(principal.getId(), status, dateFrom, dateTo, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','NETWORK','DEALER','SUBDEALER')")
    public ResponseEntity<TicketResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','NETWORK','DEALER','SUBDEALER')")
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody TicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.create(request));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','NETWORK','DEALER','SUBDEALER')")
    public ResponseEntity<TicketResponse> complete(@PathVariable Integer id) {
        return ResponseEntity.ok(ticketService.complete(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','NETWORK','DEALER','SUBDEALER')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending-count")
    @PreAuthorize("hasAnyRole('ADMIN','NETWORK','DEALER','SUBDEALER')")
    public ResponseEntity<Long> pendingCount(
            @AuthenticationPrincipal CrmUserPrincipal principal) {
        return ResponseEntity.ok(ticketService.countPending(principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody TicketRequest request) {
        return ResponseEntity.ok(ticketService.update(id, request));
    }
}