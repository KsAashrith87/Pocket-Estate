package com.selfstorage.storagedesk;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
	List<Client> findAllByOwnerOrderByNameAsc(AppUser owner);
	long countByOwner(AppUser owner);
	boolean existsByIdAndOwner(Long id, AppUser owner);
}
