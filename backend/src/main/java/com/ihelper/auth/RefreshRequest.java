package com.ihelper.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mobile clients send the refresh token here; the web app relies on the HttpOnly cookie")
public record RefreshRequest(@Schema(nullable = true) String refreshToken) {
}
