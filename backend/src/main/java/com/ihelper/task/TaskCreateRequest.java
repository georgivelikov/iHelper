package com.ihelper.task;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskCreateRequest(
		@Schema(description = "Client-generated UUID. Re-sending an existing id returns the existing task.")
		UUID id,
		@NotBlank @Size(max = 200) String title,
		@Size(max = 5000) String notes,
		@Schema(description = "Omit or null for Inbox") UUID categoryId,
		@Schema(description = "Defaults to MEDIUM") Priority priority,
		@Schema(description = "Defaults to TODO") TaskStatus status,
		LocalDate dueDate) {
}
