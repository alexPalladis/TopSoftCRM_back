package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.Dealer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface DealerRepository extends JpaRepository<Dealer, String> {
    Optional<Dealer> findByUsername(String username);
    boolean existsByAfm(String afm);

    @Query("""
        SELECT d FROM Dealer d
        LEFT JOIN FETCH d.network
        WHERE (:city IS NULL OR d.city = :city)
        AND (:networkId IS NULL OR d.network.id = :networkId)
        AND (:active IS NULL OR d.active = :active)
        AND (:search IS NULL OR d.afm LIKE %:search% OR LOWER(d.eponymia) LIKE LOWER(CONCAT('%',:search,'%')))
    """)
    Page<Dealer> findWithFilters(String city, String networkId, Boolean active, String search, Pageable pageable);

    List<Dealer> findByNetworkId(String networkId);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.dealer.id = :dealerId")
    long countCustomersByDealerId(String dealerId);
}
