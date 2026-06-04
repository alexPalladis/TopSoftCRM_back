package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.Network;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NetworkRepository extends JpaRepository<Network, String> {

    Optional<Network> findByUsername(String username);

    boolean existsByAfm(String afm);
    boolean existsByUsername(String username);

    List<Network> findAll();

    @Query("SELECT COUNT(d) FROM Dealer d WHERE d.network.id = :networkId")
    long countDealersByNetworkId(@Param("networkId") String networkId);

    @Query("""
        SELECT n FROM Network n
        WHERE (:city IS NULL OR n.city = :city)
        AND (:active IS NULL OR n.active = :active)
        AND (:search IS NULL OR n.afm LIKE %:search%
             OR LOWER(n.eponymia) LIKE LOWER(CONCAT('%',:search,'%')))
    """)
    Page<Network> findWithFilters(
            @Param("city")   String city,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}