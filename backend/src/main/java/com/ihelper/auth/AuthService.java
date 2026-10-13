package com.ihelper.auth;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ihelper.auth.AuthProperties.Registration;
import com.ihelper.category.Category;
import com.ihelper.category.CategoryRepository;
import com.ihelper.common.ConflictException;
import com.ihelper.common.ForbiddenException;
import com.ihelper.common.UnauthorizedException;

@Service
@Transactional
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private static final String BAD_CREDENTIALS = "Invalid username or password";

	private static final String BAD_REFRESH = "Invalid or expired refresh token";

	private record Seed(String name, String color) {
	}

	/** In display order; more can be added (and all reordered) by the user. */
	private static final List<Seed> STARTER_CATEGORIES = List.of(new Seed("Personal", "#6366f1"),
			new Seed("Ideas", "#ec4899"), new Seed("Work", "#0ea5e9"), new Seed("Books", "#f59e0b"),
			new Seed("Shopping", "#10b981"));

	private final UserRepository users;

	private final RefreshTokenRepository refreshTokens;

	private final CategoryRepository categories;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokens;

	private final AuthProperties properties;

	private final LoginRateLimiter limiter;

	private final Clock clock;

	/** Compared against when the username is unknown, so response time does not reveal which names exist. */
	private final String dummyHash;

	public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, CategoryRepository categories,
			PasswordEncoder passwordEncoder, TokenService tokens, AuthProperties properties, LoginRateLimiter limiter,
			Clock clock) {
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.categories = categories;
		this.passwordEncoder = passwordEncoder;
		this.tokens = tokens;
		this.properties = properties;
		this.limiter = limiter;
		this.clock = clock;
		this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	public AuthResult register(RegisterRequest request, String clientAddress) {
		String limitKey = "register|" + clientAddress;
		limiter.assertAllowed(limitKey);
		if (properties.registration() == Registration.CLOSED) {
			throw new ForbiddenException("Registration is closed");
		}
		if (properties.registration() == Registration.INVITE && !inviteMatches(request.inviteCode())) {
			limiter.recordFailure(limitKey);
			throw new ForbiddenException("A valid invite code is required to register");
		}
		String username = request.username().trim();
		if (users.existsByUsernameIgnoreCase(username)) {
			throw new ConflictException("Username '" + username + "' is already taken");
		}
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		users.saveAndFlush(user);
		for (int position = 0; position < STARTER_CATEGORIES.size(); position++) {
			Seed seed = STARTER_CATEGORIES.get(position);
			Category category = new Category();
			category.setPosition(position);
			category.setId(UUID.randomUUID());
			category.setOwnerId(user.getId());
			category.setName(seed.name());
			category.setColor(seed.color());
			categories.save(category);
		}
		log.info("Registered user {}", user.getId());
		return issue(user);
	}

	@Transactional(noRollbackFor = UnauthorizedException.class)
	public AuthResult login(LoginRequest request, String clientAddress) {
		String limitKey = "login|" + clientAddress + "|" + request.username().toLowerCase();
		limiter.assertAllowed(limitKey);
		User user = users.findByUsernameIgnoreCaseAndDeletedAtIsNull(request.username().trim()).orElse(null);
		boolean ok = passwordEncoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyHash);
		if (user == null || !ok) {
			limiter.recordFailure(limitKey);
			throw new UnauthorizedException(BAD_CREDENTIALS);
		}
		limiter.reset(limitKey);
		return issue(user);
	}

	/**
	 * Rotates: the presented token is revoked and a new one issued. Presenting an already revoked token
	 * means it leaked or was replayed, so every session of that user is ended.
	 */
	@Transactional(noRollbackFor = UnauthorizedException.class)
	public AuthResult refresh(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new UnauthorizedException(BAD_REFRESH);
		}
		Instant now = clock.instant();
		RefreshToken stored = refreshTokens.findByTokenHash(TokenService.hash(rawToken))
			.orElseThrow(() -> new UnauthorizedException(BAD_REFRESH));
		if (stored.isRevoked()) {
			refreshTokens.revokeAllForUser(stored.getUserId(), now);
			log.warn("Revoked refresh token reused; ended all sessions of user {}", stored.getUserId());
			throw new UnauthorizedException(BAD_REFRESH);
		}
		if (stored.getExpiresAt().isBefore(now)) {
			throw new UnauthorizedException(BAD_REFRESH);
		}
		User user = users.findById(stored.getUserId())
			.filter(u -> !u.isDeleted())
			.orElseThrow(() -> new UnauthorizedException(BAD_REFRESH));
		stored.revoke(now);
		refreshTokens.save(stored);
		return issue(user);
	}

	/** Idempotent: unknown or already revoked tokens are ignored. */
	public void logout(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		refreshTokens.findByTokenHash(TokenService.hash(rawToken)).ifPresent(t -> {
			if (!t.isRevoked()) {
				t.revoke(clock.instant());
				refreshTokens.save(t);
			}
		});
	}

	private AuthResult issue(User user) {
		Instant now = clock.instant();
		String raw = tokens.newRefreshToken();
		refreshTokens.save(new RefreshToken(user.getId(), TokenService.hash(raw), now, now.plus(properties.refreshTtl())));
		return new AuthResult(user, tokens.accessToken(user), tokens.accessTtlSeconds(), raw);
	}

	private boolean inviteMatches(String supplied) {
		String expected = properties.inviteCode();
		if (expected == null || expected.isBlank() || supplied == null) {
			return false;
		}
		return MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
				supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

}
