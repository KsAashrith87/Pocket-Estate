package com.selfstorage.storagedesk;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StorageUnitRepository extends JpaRepository<StorageUnit, Long> {
	long countByStatus(String status);
	List<StorageUnit> findAllByOwner(AppUser owner);
	long countByOwner(AppUser owner);
	long countByOwnerAndStatus(AppUser owner, String status);
	boolean existsByIdAndOwner(Long id, AppUser owner);
	boolean existsByClientId(Long clientId);
}
