package com.ihelper.category;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	Optional<Category> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);

	Optional<Category> findByIdAndOwnerId(UUID id, UUID ownerId);

	List<Category> findAllByOwnerIdAndDeletedAtIsNullOrderByPositionAscNameAsc(UUID ownerId);

	/** The highest position among live categories, or -1 when there are none. */
	@Query("select coalesce(max(c.position), -1) from Category c where c.ownerId = :ownerId and c.deletedAt is null")
	int maxPosition(@Param("ownerId") UUID ownerId);

	boolean existsByOwnerIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID ownerId, String name);

	boolean existsByOwnerIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(UUID ownerId, String name, UUID id);

	/** Includes soft-deleted rows: this feeds {@code /sync}. */
	List<Category> findByOwnerIdAndUpdatedAtGreaterThanOrderByUpdatedAtAsc(UUID ownerId, Instant since);

}
