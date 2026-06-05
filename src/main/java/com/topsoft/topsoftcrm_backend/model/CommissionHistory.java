package com.topsoft.topsoftcrm_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommissionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Dealer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    @Column(name = "dealer_commission_pct", precision = 5, scale = 2)
    private BigDecimal dealerCommissionPct;

    @Column(name = "dealer_commission_amount", precision = 10, scale = 2)
    private BigDecimal dealerCommissionAmount;

    @Column(name = "paid_dealer", nullable = false)
    private Boolean paidDealer = false;

    // Network (nullable — dealer μπορεί να μην έχει network)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id")
    private Network network;

    @Column(name = "network_commission_pct", precision = 5, scale = 2)
    private BigDecimal networkCommissionPct;

    @Column(name = "network_commission_amount", precision = 10, scale = 2)
    private BigDecimal networkCommissionAmount;

    @Column(name = "paid_network", nullable = false)
    private Boolean paidNetwork = false;

    @Column(name = "receipt", length = 100)
    private String receipt;

    // Πηγή: webhook από τιμολογιέρα
    @Column(name = "external_ref", length = 100)
    private String externalRef;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
