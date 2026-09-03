package com.selfstorage.storagedesk;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findTop8ByOrderByPaidOnDesc();
    List<Payment> findTop8ByOwnerOrderByPaidOnDesc(AppUser owner);
    long countByOwnerAndBillingMonth(AppUser owner, String billingMonth);
    long countByBillingMonth(String billingMonth);
    boolean existsByClientId(Long clientId);
}
