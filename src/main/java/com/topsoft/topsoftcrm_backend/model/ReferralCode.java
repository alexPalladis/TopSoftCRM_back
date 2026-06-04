package com.topsoft.topsoftcrm_backend.model;

import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_codes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReferralCode {

    @Id
    @Column(length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    @Column(name = "entity_id", length = 8, nullable = false)
    private String entityId;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
