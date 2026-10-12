package com.ihelper.task;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Full replacement: omitted optional fields are cleared. Use PATCH for partial updates. */
public record TaskUpdateRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 5000) String notes,
		@Schema(description = "Null moves the task to Inbox") UUID categoryId,
		@NotNull Priority priority,
		@NotNull TaskStatus status,
		LocalDate dueDate,
		@Schema(description = "Version last seen by the client. May instead be sent as If-Match header.")
		Long version) {
}
