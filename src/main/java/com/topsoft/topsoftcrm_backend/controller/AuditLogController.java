package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.model.AuditLog;
import com.topsoft.topsoftcrm_backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GET /api/audit-logs
 *
 * Admin-only endpoint. Επιστρέφει paginated λίστα audit entries
 * με optional φίλτρα: actorId, entityType, entityId, action, dateFrom, dateTo.
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLog>> getAll(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay()          : null;
        LocalDateTime to   = dateTo   != null ? dateTo.plusDays(1).atStartOfDay(): null;

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                auditLogRepository.findWithFilters(actorId, entityType, entityId, action, from, to, pageable)
        );
    }
}