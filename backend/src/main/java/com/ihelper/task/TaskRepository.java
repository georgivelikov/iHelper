package com.ihelper.task;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

	Optional<Task> findByIdAndDeletedAtIsNull(UUID id);

	/** Includes soft-deleted rows: this feeds {@code /sync}. */
	List<Task> findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(Instant since);

	/** Bulk update bypasses the entity lifecycle, so it bumps {@code updatedAt}/{@code version} itself. */
	@Modifying(flushAutomatically = true)
	@Query("""
			update Task t
			   set t.categoryId = null, t.updatedAt = :now, t.version = t.version + 1
			 where t.categoryId = :categoryId and t.deletedAt is null
			""")
	int moveToInbox(@Param("categoryId") UUID categoryId, @Param("now") Instant now);

}
