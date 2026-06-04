package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.SubDealer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface SubDealerRepository extends JpaRepository<SubDealer, String> {
    Optional<SubDealer> findByUsername(String username);
    boolean existsByAfm(String afm);

    @Query("""
        SELECT s FROM SubDealer s
        LEFT JOIN FETCH s.dealer
        LEFT JOIN FETCH s.network
        WHERE (:city IS NULL OR s.city = :city)
        AND (:dealerId IS NULL OR s.dealer.id = :dealerId)
        AND (:active IS NULL OR s.active = :active)
        AND (:search IS NULL OR s.afm LIKE %:search% OR LOWER(s.eponymia) LIKE LOWER(CONCAT('%',:search,'%')))
    """)
    Page<SubDealer> findWithFilters(String city, String dealerId, Boolean active, String search, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.subDealer.id = :subDealerId")
    long countCustomersBySubDealerId(String subDealerId);
    long countByDealerId(String dealerId);
}
