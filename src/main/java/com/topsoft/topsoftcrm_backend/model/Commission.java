package com.topsoft.topsoftcrm_backend.model;

import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "commissions",
        indexes = {
                // Commissions are always looked up by entity — index entity_id alone
                // and also a composite for the common query: "all commissions for entity X"
                @Index(name = "idx_commissions_entity_id", columnList = "entity_id"),
                // Composite: covers queries that filter by both entity_id AND product_id
                @Index(name = "idx_commissions_entity_product", columnList = "entity_id, product_id"),
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Commission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    @Column(name = "entity_id", length = 8, nullable = false)
    private String entityId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage = BigDecimal.ZERO;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}