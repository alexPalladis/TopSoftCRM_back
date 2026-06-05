package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.CommissionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface CommissionHistoryRepository extends JpaRepository<CommissionHistory, Long> {

    @Query("""
        SELECT c FROM CommissionHistory c
        LEFT JOIN FETCH c.product
        LEFT JOIN FETCH c.customer
        LEFT JOIN FETCH c.dealer
        LEFT JOIN FETCH c.network
        WHERE (:dateFrom IS NULL OR c.paymentDate >= :dateFrom)
        AND   (:dateTo   IS NULL OR c.paymentDate <= :dateTo)
        AND   (:productId IS NULL OR c.product.id = :productId)
        AND   (:networkId IS NULL OR c.network.id = :networkId)
        AND   (:dealerId  IS NULL OR c.dealer.id  = :dealerId)
    """)
    Page<CommissionHistory> findWithFilters(
            @Param("dateFrom")  LocalDate dateFrom,
            @Param("dateTo")    LocalDate dateTo,
            @Param("productId") Integer productId,
            @Param("networkId") String networkId,
            @Param("dealerId")  String dealerId,
            Pageable pageable
    );
}