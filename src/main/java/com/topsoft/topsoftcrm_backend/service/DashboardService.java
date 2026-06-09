package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.repository.CommissionHistoryRepository;
import com.topsoft.topsoftcrm_backend.repository.CustomerRepository;
import com.topsoft.topsoftcrm_backend.security.CrmUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CommissionHistoryRepository historyRepository;
    private final CustomerRepository          customerRepository;

    /**
     * Returns chart data for the Dashboard.
     * All data is scoped server-side based on the caller's role.
     */
    public Map<String, Object> getSummary(CrmUserPrincipal principal) {
        String role     = principal.getRole();
        String entityId = principal.getId();

        // ── Scoping ──────────────────────────────────────────────────────────
        // networkId / dealerId used to filter commission history
        String networkId = switch (role) {
            case "NETWORK" -> entityId;
            default        -> null;   // ADMIN sees all; DEALER/SUBDEALER scoped below
        };
        String dealerId = switch (role) {
            case "DEALER"    -> entityId;
            case "SUBDEALER" -> null; // subdealer has no commissions chart
            default          -> null;
        };

        Map<String, Object> result = new LinkedHashMap<>();

        // ── 1. Commissions by month (last 6 months) ──────────────────────────
        // Skip for SUBDEALER — they have no commissions view
        if (!"SUBDEALER".equals(role)) {
            result.put("commissionsByMonth", buildCommissionsByMonth(networkId, dealerId));
        }

        // ── 2. Customers by status (active vs inactive) ──────────────────────
        result.put("customersByStatus", buildCustomersByStatus(role, entityId));

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Commissions per month — last 6 months
    // Returns: [ { month: "Ιαν 25", dealer: 1200.00, network: 300.00 }, ... ]
    // ─────────────────────────────────────────────────────────────────────────
    private List<Map<String, Object>> buildCommissionsByMonth(
            String networkId, String dealerId) {

        LocalDate today     = LocalDate.now();
        LocalDate sixAgo    = today.minusMonths(5).withDayOfMonth(1);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("MMM yy", new Locale("el", "GR"));

        // Raw query: SUM dealer + network commissions per month
        // Uses existing findWithFilters for scoping — runs 6 individual range queries,
        // one per month. Simple and correct; with 500 users the volume is trivial.
        List<Map<String, Object>> months = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            LocalDate from = today.minusMonths(5 - i).withDayOfMonth(1);
            LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());

            // Use pageable size=10000 to get all records for this month
            var page = historyRepository.findWithFilters(
                    from, to, null, networkId, dealerId,
                    org.springframework.data.domain.PageRequest.of(0, 10_000));

            BigDecimal dealerTotal  = BigDecimal.ZERO;
            BigDecimal networkTotal = BigDecimal.ZERO;

            for (var h : page.getContent()) {
                if (h.getDealerCommissionAmount()  != null) dealerTotal  = dealerTotal.add(h.getDealerCommissionAmount());
                if (h.getNetworkCommissionAmount() != null) networkTotal = networkTotal.add(h.getNetworkCommissionAmount());
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month",   from.format(f));
            m.put("dealer",  dealerTotal.setScale(2, RoundingMode.HALF_UP));
            m.put("network", networkTotal.setScale(2, RoundingMode.HALF_UP));
            months.add(m);
        }

        return months;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Customers by status — active vs inactive, scoped by role
    // Returns: { active: 42, inactive: 8 }
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> buildCustomersByStatus(String role, String entityId) {
        long active;
        long inactive;

        switch (role) {
            case "DEALER" -> {
                // Count only customers belonging to this dealer
                var all = customerRepository.findWithFilters(
                        null, entityId, null, null, null,
                        org.springframework.data.domain.PageRequest.of(0, 1));
                var act = customerRepository.findWithFilters(
                        null, entityId, null, true, null,
                        org.springframework.data.domain.PageRequest.of(0, 1));
                active   = act.getTotalElements();
                inactive = all.getTotalElements() - active;
            }
            case "NETWORK" -> {
                var all = customerRepository.findWithFilters(
                        null, null, entityId, null, null,
                        org.springframework.data.domain.PageRequest.of(0, 1));
                var act = customerRepository.findWithFilters(
                        null, null, entityId, true, null,
                        org.springframework.data.domain.PageRequest.of(0, 1));
                active   = act.getTotalElements();
                inactive = all.getTotalElements() - active;
            }
            case "SUBDEALER" -> {
                long subDealerId_customerCount = customerRepository.countBySubDealerId(entityId);
                // For subdealer, count active via a dedicated query isn't exposed —
                // return totals only (active = total, inactive = 0 as fallback)
                active   = subDealerId_customerCount;
                inactive = 0;
            }
            default -> {
                // ADMIN — all customers
                long total = customerRepository.count();
                var act = customerRepository.findWithFilters(
                        null, null, null, true, null,
                        org.springframework.data.domain.PageRequest.of(0, 1));
                active   = act.getTotalElements();
                inactive = total - active;
            }
        }

        return Map.of("active", active, "inactive", inactive);
    }
}