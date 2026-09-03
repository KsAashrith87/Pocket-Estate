package com.selfstorage.storagedesk;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentPaymentRepository extends JpaRepository<RentPayment, Long> {
    List<RentPayment> findTop8ByOwnerOrderByPaidOnDesc(AppUser owner);
    long countByOwnerAndBillingMonth(AppUser owner, String billingMonth);
}
