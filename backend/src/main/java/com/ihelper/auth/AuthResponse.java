package com.ihelper.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = { "accessToken", "tokenType", "expiresIn", "user" })
public record AuthResponse(
		String accessToken,
		String tokenType,
		@Schema(description = "Access token lifetime in seconds") long expiresIn,
		@Schema(description = "Only present when the request sent X-Token-Transport: body (mobile). "
				+ "The web app gets it as an HttpOnly cookie instead.", nullable = true)
		String refreshToken,
		UserDto user) {
}
