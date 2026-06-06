package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, String> {

    Optional<Customer> findByAfm(String afm);
    boolean existsByAfm(String afm);

    long countByDealerId(String dealerId);
    long countBySubDealerId(String subDealerId);

    // Network count — no stored column, join through dealer
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.dealer.network.id = :networkId")
    long countByNetworkId(String networkId);

    // ----------------------------------------------------------------- QUERIES

    // Used by ADMIN / NETWORK / DEALER roles.
    // Network filter works by joining dealer → dealer.network (no stored network_id).
    @Query("""
        SELECT c FROM Customer c
        JOIN FETCH c.dealer d
        LEFT JOIN FETCH d.network
        LEFT JOIN FETCH c.subDealer
        WHERE (:city      IS NULL OR c.city        = :city)
          AND (:dealerId  IS NULL OR d.id           = :dealerId)
          AND (:networkId IS NULL OR d.network.id   = :networkId)
          AND (:active    IS NULL OR c.active        = :active)
          AND (:search    IS NULL
               OR c.afm LIKE %:search%
               OR LOWER(c.eponymia) LIKE LOWER(CONCAT('%',:search,'%')))
    """)
    Page<Customer> findWithFilters(
            String city,
            String dealerId,
            String networkId,
            Boolean active,
            String search,
            Pageable pageable);

    // Used by SUBDEALER role — sees only their own customers
    @Query("""
        SELECT c FROM Customer c
        JOIN FETCH c.dealer d
        LEFT JOIN FETCH d.network
        LEFT JOIN FETCH c.subDealer sd
        WHERE sd.id = :subDealerId
          AND (:city   IS NULL OR c.city   = :city)
          AND (:active IS NULL OR c.active = :active)
          AND (:search IS NULL
               OR c.afm LIKE %:search%
               OR LOWER(c.eponymia) LIKE LOWER(CONCAT('%',:search,'%')))
    """)
    Page<Customer> findBySubDealerId(
            String subDealerId,
            String city,
            Boolean active,
            String search,
            Pageable pageable);
}