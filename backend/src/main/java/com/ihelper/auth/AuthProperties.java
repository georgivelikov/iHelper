package com.ihelper.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code ihelper.auth.*}. The JWT secret has a development default only in the base profile; the
 * {@code prod} profile has none, so a missing secret stops the app at startup.
 */
@ConfigurationProperties("ihelper.auth")
public record AuthProperties(
		String jwtSecret,
		Duration accessTtl,
		Duration refreshTtl,
		Registration registration,
		String inviteCode,
		boolean secureCookie) {

	public enum Registration {
		/** Anyone can register. For local development and tests. */
		OPEN,
		/** Registering needs the shared invite code. */
		INVITE,
		/** No new accounts. */
		CLOSED
	}

	public AuthProperties {
		if (accessTtl == null) {
			accessTtl = Duration.ofMinutes(15);
		}
		if (refreshTtl == null) {
			refreshTtl = Duration.ofDays(30);
		}
		if (registration == null) {
			registration = Registration.INVITE;
		}
		if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException(
					"ihelper.auth.jwt-secret (IHELPER_JWT_SECRET) must be set and at least 32 bytes long");
		}
	}

}
