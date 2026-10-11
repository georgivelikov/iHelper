package com.ihelper.auth;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Auth")
public class MeController {

	@GetMapping
	@Operation(summary = "The signed-in user")
	public UserDto me(@AuthenticationPrincipal Jwt jwt) {
		return new UserDto(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("username"));
	}

}
