package com.ihelper.category;

import java.time.Instant;
import java.util.UUID;

public record CategoryDto(UUID id, String name, String color, Instant createdAt, Instant updatedAt, Long version,
		Instant deletedAt) {

	public static CategoryDto from(Category c) {
		return new CategoryDto(c.getId(), c.getName(), c.getColor(), c.getCreatedAt(), c.getUpdatedAt(),
				c.getVersion(), c.getDeletedAt());
	}

}
