package com.ihelper.category;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	Optional<Category> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);

	Optional<Category> findByIdAndOwnerId(UUID id, UUID ownerId);

	List<Category> findAllByOwnerIdAndDeletedAtIsNullOrderByNameAsc(UUID ownerId);

	boolean existsByOwnerIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID ownerId, String name);

	boolean existsByOwnerIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(UUID ownerId, String name, UUID id);

	/** Includes soft-deleted rows: this feeds {@code /sync}. */
	List<Category> findByOwnerIdAndUpdatedAtGreaterThanOrderByUpdatedAtAsc(UUID ownerId, Instant since);

}
