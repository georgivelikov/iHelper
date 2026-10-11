package com.ihelper.category;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	Optional<Category> findByIdAndDeletedAtIsNull(UUID id);

	List<Category> findAllByDeletedAtIsNullOrderByNameAsc();

	boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

	boolean existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(String name, UUID id);

	/** Includes soft-deleted rows: this feeds {@code /sync}. */
	List<Category> findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(Instant since);

}
