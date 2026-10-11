package com.ihelper.category;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = { "id", "name", "createdAt", "updatedAt", "version" })
public record CategoryDto(UUID id, String name,
		@Schema(nullable = true) String color,
		Instant createdAt, Instant updatedAt, Long version,
		@Schema(nullable = true) Instant deletedAt) {

	public static CategoryDto from(Category c) {
		return new CategoryDto(c.getId(), c.getName(), c.getColor(), c.getCreatedAt(), c.getUpdatedAt(),
				c.getVersion(), c.getDeletedAt());
	}

}
