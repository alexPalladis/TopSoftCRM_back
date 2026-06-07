package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.SubDealer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubDealerRepository extends JpaRepository<SubDealer, String> {

    Optional<SubDealer> findByUsername(String username);
    boolean existsByAfm(String afm);
    long countByDealerId(String dealerId);

    List<SubDealer> findAllByActiveTrueOrderByEponymiaAsc();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.subDealer.id = :subDealerId")
    long countCustomersBySubDealerId(String subDealerId);

    // Network filtering: join dealer, then dealer.network — no stored network_id
    @Query("""
        SELECT s FROM SubDealer s
        JOIN FETCH s.dealer d
        LEFT JOIN FETCH d.network
        WHERE (:city      IS NULL OR s.city        = :city)
          AND (:dealerId  IS NULL OR d.id           = :dealerId)
          AND (:networkId IS NULL OR d.network.id   = :networkId)
          AND (:active    IS NULL OR s.active        = :active)
          AND (:search    IS NULL
               OR s.afm LIKE %:search%
               OR LOWER(s.eponymia) LIKE LOWER(CONCAT('%',:search,'%')))
    """)
    Page<SubDealer> findWithFilters(
            String city,
            String dealerId,
            String networkId,
            Boolean active,
            String search,
            Pageable pageable);

    // Για NETWORK — subdealers του δικτύου μέσω dealer
    @Query("""
    SELECT s FROM SubDealer s
    JOIN s.dealer d
    WHERE d.network.id = :networkId
    AND s.active = true
    ORDER BY s.eponymia ASC
""")
    List<SubDealer> findActiveByNetworkId(@Param("networkId") String networkId);

    // Για DEALER — subdealers που ανήκουν σε αυτόν τον dealer
    List<SubDealer> findByDealerIdAndActiveTrueOrderByEponymiaAsc(String dealerId);
}