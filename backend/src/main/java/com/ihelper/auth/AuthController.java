package com.ihelper.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
@SecurityRequirements
public class AuthController {

	static final String REFRESH_COOKIE = "ihelper_refresh";

	static final String TRANSPORT_HEADER = "X-Token-Transport";

	private final AuthService service;

	private final AuthProperties properties;

	public AuthController(AuthService service, AuthProperties properties) {
		this.service = service;
		this.properties = properties;
	}

	@PostMapping("/register")
	@Operation(summary = "Create an account and sign in",
			description = "403 when registration is closed or the invite code is wrong, 409 when the username is taken.")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
			@Parameter(description = "Send 'body' to receive the refresh token in the response body (mobile)")
			@RequestHeader(value = TRANSPORT_HEADER, required = false) String transport,
			HttpServletRequest http) {
		return respond(HttpStatus.CREATED, service.register(request, http.getRemoteAddr()), transport);
	}

	@PostMapping("/login")
	@Operation(summary = "Sign in with username and password",
			description = "401 on bad credentials, 429 after repeated failures.")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
			@RequestHeader(value = TRANSPORT_HEADER, required = false) String transport, HttpServletRequest http) {
		return respond(HttpStatus.OK, service.login(request, http.getRemoteAddr()), transport);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Trade a refresh token for a new access token (the refresh token rotates)",
			description = "The token comes from the request body (mobile) or the HttpOnly cookie (web).")
	public ResponseEntity<AuthResponse> refresh(@RequestBody(required = false) RefreshRequest body,
			@CookieValue(value = REFRESH_COOKIE, required = false) String cookie,
			@RequestHeader(value = TRANSPORT_HEADER, required = false) String transport) {
		String token = body != null && body.refreshToken() != null ? body.refreshToken() : cookie;
		return respond(HttpStatus.OK, service.refresh(token), transport);
	}

	@PostMapping("/logout")
	@Operation(summary = "Revoke the refresh token and clear the cookie; idempotent")
	public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest body,
			@CookieValue(value = REFRESH_COOKIE, required = false) String cookie) {
		service.logout(body != null && body.refreshToken() != null ? body.refreshToken() : cookie);
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
			.build();
	}

	private ResponseEntity<AuthResponse> respond(HttpStatus status, AuthResult result, String transport) {
		boolean bodyTransport = "body".equalsIgnoreCase(transport);
		var response = new AuthResponse(result.accessToken(), "Bearer", result.expiresIn(),
				bodyTransport ? result.refreshToken() : null, UserDto.from(result.user()));
		var builder = ResponseEntity.status(status).header(HttpHeaders.CACHE_CONTROL, "no-store");
		if (!bodyTransport) {
			builder.header(HttpHeaders.SET_COOKIE,
					refreshCookie(result.refreshToken(), properties.refreshTtl().toSeconds()).toString());
		}
		return builder.body(response);
	}

	private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
		return ResponseCookie.from(REFRESH_COOKIE, value)
			.httpOnly(true)
			.secure(properties.secureCookie())
			.sameSite("Strict")
			.path("/api/v1/auth")
			.maxAge(maxAgeSeconds)
			.build();
	}

}
