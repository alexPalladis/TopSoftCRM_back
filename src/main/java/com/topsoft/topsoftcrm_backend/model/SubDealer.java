package com.topsoft.topsoftcrm_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "subdealers",
        indexes = {
                // The ONLY FK a subdealer needs — its direct parent.
                // Network is always derived via: subdealer → dealer → network
                @Index(name = "idx_subdealers_dealer_id", columnList = "dealer_id"),
                @Index(name = "idx_subdealers_city",      columnList = "city"),
                @Index(name = "idx_subdealers_active",    columnList = "active"),
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubDealer {

    @Id
    @Column(length = 8)
    private String id;

    @Column(length = 9, nullable = false, unique = true)
    private String afm;

    @Column(length = 200, nullable = false)
    private String eponymia;

    @Column(name = "nomimos_ekprosopos", length = 200)
    private String nomimosEkprosopos;

    @Column(length = 100, nullable = false)
    private String epaggelma;

    @Column(length = 100, nullable = false)
    private String doy;

    @Column(length = 300, nullable = false)
    private String address;

    @Column(length = 100, nullable = false)
    private String city;

    @Column(length = 5, nullable = false)
    private String tk;

    @Column(name = "phone_fixed", length = 20)
    private String phoneFixed;

    @Column(name = "phone_mobile", length = 20, nullable = false)
    private String phoneMobile;

    @Column(length = 150, nullable = false)
    private String email;

    @Column(length = 100, nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    // ONLY direct parent — network is resolved at query time via dealer.network
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // -----------------------------------------------------------------------
    // Convenience accessor — never stored, always derived. Safe to call
    // only when dealer is already loaded (LAZY, so call within a transaction).
    // -----------------------------------------------------------------------
    public Network getNetwork() {
        return dealer != null ? dealer.getNetwork() : null;
    }
}