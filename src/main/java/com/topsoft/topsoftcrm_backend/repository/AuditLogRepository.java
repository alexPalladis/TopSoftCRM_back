package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:actorId    IS NULL OR a.actorId    = :actorId)
          AND (:entityType IS NULL OR a.entityType = :entityType)
          AND (:entityId   IS NULL OR a.entityId   = :entityId)
          AND (:action     IS NULL OR a.action     = :action)
          AND (:dateFrom   IS NULL OR a.createdAt >= :dateFrom)
          AND (:dateTo     IS NULL OR a.createdAt <= :dateTo)
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLog> findWithFilters(
            @Param("actorId")    String actorId,
            @Param("entityType") String entityType,
            @Param("entityId")   String entityId,
            @Param("action")     String action,
            @Param("dateFrom")   LocalDateTime dateFrom,
            @Param("dateTo")     LocalDateTime dateTo,
            Pageable pageable
    );
}