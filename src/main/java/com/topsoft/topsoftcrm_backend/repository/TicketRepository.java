package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.Ticket;
import com.topsoft.topsoftcrm_backend.model.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    /**
     * Returns all tickets belonging to an entity (sent or received),
     * with optional server-side filtering by status and date range.
     *
     * Using :status IS NULL trick: when the caller passes null,
     * the condition is skipped entirely — no extra query method needed.
     * Same pattern for dateFrom / dateTo.
     *
     * LocalDate comparisons work because createdAt is LocalDateTime —
     * CAST(t.createdAt AS LocalDate) normalises it for the comparison.
     */
    @Query("""
        SELECT t FROM Ticket t
        WHERE (t.fromId = :entityId OR t.toId = :entityId)
          AND (:status   IS NULL OR t.status = :status)
          AND (:dateFrom IS NULL OR CAST(t.createdAt AS LocalDate) >= :dateFrom)
          AND (:dateTo   IS NULL OR CAST(t.createdAt AS LocalDate) <= :dateTo)
        ORDER BY t.createdAt DESC
    """)
    Page<Ticket> findByEntityFiltered(
            @Param("entityId") String entityId,
            @Param("status")   TicketStatus status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo")   LocalDate dateTo,
            Pageable pageable
    );

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.toId = :entityId AND t.status = 'PENDING'")
    long countPendingByEntityId(String entityId);
}