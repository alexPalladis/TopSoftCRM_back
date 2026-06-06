package com.topsoft.topsoftcrm_backend.model;

import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import com.topsoft.topsoftcrm_backend.model.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tickets",
        indexes = {
                // The main query filters by (fromId OR toId) — both need indexes
                @Index(name = "idx_tickets_from_id", columnList = "from_id"),
                @Index(name = "idx_tickets_to_id",   columnList = "to_id"),
                // Used in the new status filter and the pending-count query
                @Index(name = "idx_tickets_status",  columnList = "status"),
                // Used for date range filter and ORDER BY created_at DESC
                @Index(name = "idx_tickets_created_at", columnList = "created_at"),
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_type", nullable = false)
    private EntityType fromType;

    @Column(name = "from_id", length = 8, nullable = false)
    private String fromId;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_type", nullable = false)
    private EntityType toType;

    @Column(name = "to_id", length = 8, nullable = false)
    private String toId;

    @Column(length = 300, nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}