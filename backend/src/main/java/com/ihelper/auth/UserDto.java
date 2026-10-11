package com.ihelper.auth;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = { "id", "username" })
public record UserDto(UUID id, String username) {

	public static UserDto from(User u) {
		return new UserDto(u.getId(), u.getUsername());
	}

}
