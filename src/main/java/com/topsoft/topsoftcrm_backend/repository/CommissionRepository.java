package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.Commission;
import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommissionRepository extends JpaRepository<Commission, Integer> {

    List<Commission> findByEntityTypeAndEntityId(EntityType entityType, String entityId);

    Optional<Commission> findByEntityTypeAndEntityIdAndProductId(
            EntityType entityType, String entityId, Integer productId);

    void deleteByEntityTypeAndEntityId(EntityType entityType, String entityId);
}
