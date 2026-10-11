package com.ihelper.auth;

/** What a successful authentication hands back; the controller decides how the refresh token travels. */
public record AuthResult(User user, String accessToken, long expiresIn, String refreshToken) {
}
