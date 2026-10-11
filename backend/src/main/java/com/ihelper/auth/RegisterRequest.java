package com.ihelper.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(min = 3, max = 50)
		@Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "may contain letters, digits, dot, underscore and dash")
		String username,
		@NotBlank @Size(min = 10, max = 72, message = "must be 10-72 characters") String password,
		@Schema(description = "Required when the server runs registration in invite mode", nullable = true)
		String inviteCode) {
}
