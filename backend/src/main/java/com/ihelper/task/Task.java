package com.ihelper.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.ihelper.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "task")
public class Task extends BaseEntity {

	@Column(name = "owner_id", nullable = false, updatable = false)
	private UUID ownerId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 5000)
	private String description;

	/** Null means Inbox. A plain id rather than an association: DTOs only ever need the id. */
	@Column(name = "category_id")
	private UUID categoryId;

	@Enumerated(EnumType.ORDINAL)
	@Column(nullable = false)
	private Priority priority = Priority.MEDIUM;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskStatus status = TaskStatus.TODO;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Column(name = "completed_at")
	private Instant completedAt;

	/** Set when the bin was emptied: the content is wiped and only a tombstone remains for /sync. */
	@Column(name = "purged_at")
	private Instant purgedAt;

	public UUID getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UUID ownerId) {
		this.ownerId = ownerId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(UUID categoryId) {
		this.categoryId = categoryId;
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(Instant completedAt) {
		this.completedAt = completedAt;
	}

	public Instant getPurgedAt() {
		return purgedAt;
	}

	public void setPurgedAt(Instant purgedAt) {
		this.purgedAt = purgedAt;
	}

}
