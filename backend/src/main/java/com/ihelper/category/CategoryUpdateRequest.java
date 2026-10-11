package com.ihelper.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
		@NotBlank @Size(max = 100) String name,
		@Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex color like #1a2b3c") String color,
		@Schema(description = "Version last seen by the client. May instead be sent as If-Match header.")
		Long version) {
}
