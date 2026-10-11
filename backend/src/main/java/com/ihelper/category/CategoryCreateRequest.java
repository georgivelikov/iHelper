package com.ihelper.category;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
		@Schema(description = "Client-generated UUID. Re-sending an existing id returns the existing category.")
		UUID id,
		@NotBlank @Size(max = 100) String name,
		@Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex color like #1a2b3c") String color) {
}
