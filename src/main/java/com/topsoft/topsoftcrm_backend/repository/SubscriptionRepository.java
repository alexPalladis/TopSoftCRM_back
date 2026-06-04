package com.topsoft.topsoftcrm_backend.repository;

import com.topsoft.topsoftcrm_backend.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {
    List<Subscription> findByCustomerId(String customerId);
    Optional<Subscription> findByCustomerIdAndProductId(String customerId, Integer productId);
    boolean existsByCustomerIdAndProductId(String customerId, Integer productId);
}
