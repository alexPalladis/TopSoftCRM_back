package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.ReferralCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReferralCodeRepository extends JpaRepository<ReferralCode, String> {
    Optional<ReferralCode> findByCodeAndActiveTrue(String code);
}
