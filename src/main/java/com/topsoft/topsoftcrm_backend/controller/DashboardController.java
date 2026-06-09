package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import com.topsoft.topsoftcrm_backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GET /api/dashboard/summary
 *
 * Επιστρέφει δεδομένα για τα γραφήματα του Dashboard:
 *  - commissionsByMonth : προμήθειες ανά μήνα (τελευταίοι 6 μήνες)
 *  - customersByStatus  : ενεργοί vs ανενεργοί πελάτες
 *  - topProducts        : top 5 προϊόντα βάσει εσόδων (admin only)
 *
 * Scoped server-side ανά ρόλο — dealer βλέπει μόνο τα δικά του,
 * network τα δικά του, admin τα πάντα.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','NETWORK','DEALER','SUBDEALER')")
    public ResponseEntity<Map<String, Object>> getSummary(
            @AuthenticationPrincipal CrmUserPrincipal principal) {
        return ResponseEntity.ok(dashboardService.getSummary(principal));
    }
}