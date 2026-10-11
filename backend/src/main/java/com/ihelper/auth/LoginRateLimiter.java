package com.ihelper.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.ihelper.common.TooManyRequestsException;

/**
 * Failed-attempt limiter per key (client address + username). In memory, so it protects one instance;
 * move it to the database or a gateway before running several.
 */
@Component
public class LoginRateLimiter {

	static final int MAX_FAILURES = 8;

	static final Duration WINDOW = Duration.ofMinutes(15);

	private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

	private final Clock clock;

	public LoginRateLimiter(Clock clock) {
		this.clock = clock;
	}

	public void assertAllowed(String key) {
		Deque<Instant> window = failures.get(key);
		if (window == null) {
			return;
		}
		Instant now = clock.instant();
		synchronized (window) {
			prune(window, now);
			if (window.size() >= MAX_FAILURES) {
				long wait = Duration.between(now, window.peekFirst().plus(WINDOW)).toSeconds() + 1;
				throw new TooManyRequestsException(Math.max(wait, 1));
			}
		}
	}

	public void recordFailure(String key) {
		Deque<Instant> window = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
		synchronized (window) {
			Instant now = clock.instant();
			prune(window, now);
			window.addLast(now);
		}
		if (failures.size() > 10_000) {
			failures.entrySet().removeIf(e -> {
				synchronized (e.getValue()) {
					prune(e.getValue(), clock.instant());
					return e.getValue().isEmpty();
				}
			});
		}
	}

	public void reset(String key) {
		failures.remove(key);
	}

	private static void prune(Deque<Instant> window, Instant now) {
		while (!window.isEmpty() && window.peekFirst().plus(WINDOW).isBefore(now)) {
			window.removeFirst();
		}
	}

}
