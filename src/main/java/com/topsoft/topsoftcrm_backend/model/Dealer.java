package com.topsoft.topsoftcrm_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "dealers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Dealer {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id")
    private Network network;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}