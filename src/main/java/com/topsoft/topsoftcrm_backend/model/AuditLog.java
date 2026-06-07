package com.topsoft.topsoftcrm_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AuditLog — καταγράφει κάθε σημαντική ενέργεια στο σύστημα.
 *
 * Χρησιμοποιείται από AuditLogService που καλείται από τους
 * υπόλοιπους services (DealerService, CustomerService, κ.λπ.)
 * αμέσως μετά από κάθε CREATE / UPDATE / DELETE.
 *
 * Δεν είναι @Transactional — γράφεται σε ξεχωριστό flush
 * ώστε να μην επηρεαστεί από rollback της κύριας πράξης.
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_actor_id",    columnList = "actor_id"),
                @Index(name = "idx_audit_entity_type", columnList = "entity_type"),
                @Index(name = "idx_audit_entity_id",   columnList = "entity_id"),
                @Index(name = "idx_audit_created_at",  columnList = "created_at"),
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ποιος έκανε την ενέργεια (id 8 ψηφίων) */
    @Column(name = "actor_id", length = 8, nullable = false)
    private String actorId;

    /** Ρόλος αυτού που έκανε την ενέργεια */
    @Column(name = "actor_role", length = 20, nullable = false)
    private String actorRole;

    /** Εμφανίσιμο όνομα (username ή επωνυμία) */
    @Column(name = "actor_name", length = 200)
    private String actorName;

    /** Ποιο entity επηρεάστηκε: CUSTOMER, DEALER, NETWORK, SUBDEALER, COMMISSION κ.λπ. */
    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    /** ID του entity που επηρεάστηκε */
    @Column(name = "entity_id", length = 50)
    private String entityId;

    /** Εμφανίσιμο όνομα/ΑΦΜ του entity */
    @Column(name = "entity_name", length = 300)
    private String entityName;

    /** CREATE / UPDATE / DELETE / LOGIN / REASSIGN κ.λπ. */
    @Column(name = "action", length = 50, nullable = false)
    private String action;

    /** Ελεύθερο κείμενο περιγραφής */
    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}