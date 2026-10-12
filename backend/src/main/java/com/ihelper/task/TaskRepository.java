package com.ihelper.task;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

	Optional<Task> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);

	Optional<Task> findByIdAndOwnerId(UUID id, UUID ownerId);

	/** The bin: soft-deleted tasks that have not been emptied yet. */
	Page<Task> findByOwnerIdAndDeletedAtIsNotNullAndPurgedAtIsNull(UUID ownerId, Pageable pageable);

	long countByOwnerIdAndDeletedAtIsNotNullAndPurgedAtIsNull(UUID ownerId);

	/** Includes soft-deleted rows: this feeds {@code /sync}. */
	List<Task> findByOwnerIdAndUpdatedAtGreaterThanOrderByUpdatedAtAsc(UUID ownerId, Instant since);

	interface CategoryCount {

		UUID getCategoryId();

		long getOpen();

	}

	/** Open (not DONE) live tasks per category; the null category is Inbox. */
	@Query("""
			select t.categoryId as categoryId, count(t) as open
			  from Task t
			 where t.ownerId = :ownerId and t.deletedAt is null and t.status <> com.ihelper.task.TaskStatus.DONE
			 group by t.categoryId
			""")
	List<CategoryCount> countOpenByCategory(@Param("ownerId") UUID ownerId);

	/** Bulk update bypasses the entity lifecycle, so it bumps {@code updatedAt}/{@code version} itself. */
	@Modifying(flushAutomatically = true)
	@Query("""
			update Task t
			   set t.categoryId = null, t.updatedAt = :now, t.version = t.version + 1
			 where t.ownerId = :ownerId and t.categoryId = :categoryId and t.deletedAt is null
			""")
	int moveToInbox(@Param("ownerId") UUID ownerId, @Param("categoryId") UUID categoryId, @Param("now") Instant now);

	/**
	 * Empties the bin: wipes the content of every binned task and leaves a tombstone so {@code /sync} still
	 * reports it. Bulk update, so it bumps {@code updatedAt}/{@code version} itself.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Task t
			   set t.title = '', t.notes = null, t.categoryId = null, t.dueDate = null, t.completedAt = null,
			       t.purgedAt = :now, t.updatedAt = :now, t.version = t.version + 1
			 where t.ownerId = :ownerId and t.deletedAt is not null and t.purgedAt is null
			""")
	int purgeBin(@Param("ownerId") UUID ownerId, @Param("now") Instant now);

	/** Like {@link #purgeBin} but only for the given tasks, and only those that really are in the bin. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Task t
			   set t.title = '', t.notes = null, t.categoryId = null, t.dueDate = null, t.completedAt = null,
			       t.purgedAt = :now, t.updatedAt = :now, t.version = t.version + 1
			 where t.ownerId = :ownerId and t.id in :ids and t.deletedAt is not null and t.purgedAt is null
			""")
	int purgeBinTasks(@Param("ownerId") UUID ownerId, @Param("ids") java.util.Collection<UUID> ids,
			@Param("now") Instant now);

}
