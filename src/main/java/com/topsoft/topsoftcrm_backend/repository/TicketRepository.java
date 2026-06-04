package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.fromId = :entityId OR t.toId = :entityId
        ORDER BY t.createdAt DESC
    """)
    Page<Ticket> findByEntity(String entityId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.toId = :entityId AND t.status = 'PENDING'")
    long countPendingByEntityId(String entityId);
}
