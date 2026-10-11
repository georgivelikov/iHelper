package com.ihelper.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = { "id", "title", "priority", "status", "createdAt", "updatedAt", "version" })
public record TaskDto(UUID id, String title,
		@Schema(nullable = true) String description,
		@Schema(nullable = true, description = "Null means Inbox") UUID categoryId,
		Priority priority, TaskStatus status,
		@Schema(nullable = true) LocalDate dueDate,
		@Schema(nullable = true) Instant completedAt,
		Instant createdAt, Instant updatedAt, Long version,
		@Schema(nullable = true, description = "Set when soft-deleted (only visible through /sync)") Instant deletedAt) {

	public static TaskDto from(Task t) {
		return new TaskDto(t.getId(), t.getTitle(), t.getDescription(), t.getCategoryId(), t.getPriority(),
				t.getStatus(), t.getDueDate(), t.getCompletedAt(), t.getCreatedAt(), t.getUpdatedAt(),
				t.getVersion(), t.getDeletedAt());
	}

}
