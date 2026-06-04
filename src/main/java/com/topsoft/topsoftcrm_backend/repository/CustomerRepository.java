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
    long countByNetworkId(String networkId);
    long countByDealerId(String dealerId);
    long countBySubDealerId(String subDealerId);

    @Query("""
        SELECT c FROM Customer c
        LEFT JOIN FETCH c.dealer
        LEFT JOIN FETCH c.subDealer
        LEFT JOIN FETCH c.network
        WHERE (:city IS NULL OR c.city = :city)
        AND (:dealerId IS NULL OR c.dealer.id = :dealerId)
        AND (:networkId IS NULL OR c.network.id = :networkId)
        AND (:active IS NULL OR c.active = :active)
        AND (:search IS NULL OR c.afm LIKE %:search% OR LOWER(c.eponymia) LIKE LOWER(CONCAT('%',:search,'%')))
    """)
    Page<Customer> findWithFilters(String city, String dealerId, String networkId, Boolean active, String search, Pageable pageable);
}