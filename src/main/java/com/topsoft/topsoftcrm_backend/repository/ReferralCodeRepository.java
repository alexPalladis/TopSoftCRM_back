package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.ReferralCode;
import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferralCodeRepository extends JpaRepository<ReferralCode, String> {

    // Used by WebhookService to resolve referral code → dealer/subdealer
    Optional<ReferralCode> findByCodeAndActiveTrue(String code);

    // Used by ReferralCodeController — fetch the active code for a specific entity
    Optional<ReferralCode> findByEntityIdAndActiveTrue(String entityId);

    // Used to check if a code already exists before creating
    boolean existsByEntityIdAndActiveTrue(String entityId);
}