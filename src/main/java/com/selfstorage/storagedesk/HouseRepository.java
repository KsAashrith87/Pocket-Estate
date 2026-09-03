package com.selfstorage.storagedesk;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseRepository extends JpaRepository<House, Long> {
    List<House> findAllByOwner(AppUser owner);
    long countByOwner(AppUser owner);
    long countByOwnerAndStatus(AppUser owner, String status);
    boolean existsByIdAndOwner(Long id, AppUser owner);
}
