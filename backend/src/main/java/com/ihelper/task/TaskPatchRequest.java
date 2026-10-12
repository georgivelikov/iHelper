package com.ihelper.task;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Partial update. Only fields present in the JSON are applied; an explicit {@code null} clears
 * {@code notes}, {@code categoryId} (Inbox) and {@code dueDate}. Presence is read from the raw
 * JSON, which is why this record is the typed view and not the controller's parameter type.
 */
@Schema(description = "Only the fields present are changed. null clears description, categoryId and dueDate.")
public record TaskPatchRequest(String title, String notes, UUID categoryId, Priority priority,
		TaskStatus status, LocalDate dueDate,
		@Schema(description = "Optional guard; may instead be sent as If-Match header") Long version) {

	/** JSON property names a PATCH body may carry (besides version). */
	static final java.util.List<String> FIELDS = java.util.List.of("title", "notes", "categoryId", "priority",
			"status", "dueDate");

}
