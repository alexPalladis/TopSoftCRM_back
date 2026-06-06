package com.topsoft.topsoftcrm_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "customers",
        indexes = {
                // dealer_id is the primary FK for all customer filtering
                @Index(name = "idx_customers_dealer_id",    columnList = "dealer_id"),
                // subdealer_id used when filtering "my customers" from subdealer role
                @Index(name = "idx_customers_subdealer_id", columnList = "subdealer_id"),
                // network is derived via dealer — no network_id column stored here
                @Index(name = "idx_customers_city",         columnList = "city"),
                @Index(name = "idx_customers_active",       columnList = "active"),
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {

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

    @Column(nullable = false)
    private Boolean active = true;

    // Mandatory — every customer must have a dealer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    // Optional — customer may or may not belong to a subdealer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subdealer_id")
    private SubDealer subDealer;

    // network_id is NOT stored here — derived at query time via dealer.network
    // This eliminates stale data when admin moves a dealer to a different network

    @Column(length = 10, nullable = false)
    private String source = "MANUAL";

    @Column(name = "referral_code", length = 20)
    private String referralCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // -----------------------------------------------------------------------
    // Convenience accessor — never stored, always derived.
    // -----------------------------------------------------------------------
    public Network getNetwork() {
        return dealer != null ? dealer.getNetwork() : null;
    }
}