package com.ihelper.common;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** The authenticated user of the current request; every service scopes its data with {@link #id()}. */
@Component
public class CurrentUser {

	public UUID id() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
			return UUID.fromString(jwt.getSubject());
		}
		throw new IllegalStateException("No authenticated user in the current request");
	}

}
