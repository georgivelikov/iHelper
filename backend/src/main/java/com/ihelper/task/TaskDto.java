package com.ihelper.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskDto(UUID id, String title, String description, UUID categoryId, Priority priority,
		TaskStatus status, LocalDate dueDate, Instant completedAt, Instant createdAt, Instant updatedAt,
		Long version, Instant deletedAt) {

	public static TaskDto from(Task t) {
		return new TaskDto(t.getId(), t.getTitle(), t.getDescription(), t.getCategoryId(), t.getPriority(),
				t.getStatus(), t.getDueDate(), t.getCompletedAt(), t.getCreatedAt(), t.getUpdatedAt(),
				t.getVersion(), t.getDeletedAt());
	}

}
