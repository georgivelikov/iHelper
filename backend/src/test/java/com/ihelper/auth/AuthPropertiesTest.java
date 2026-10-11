package com.ihelper.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.ihelper.auth.AuthProperties.Registration;

class AuthPropertiesTest {

	private static final String SECRET = "0123456789abcdef0123456789abcdef";

	@Test
	void appliesSafeDefaults() {
		var props = new AuthProperties(SECRET, null, null, null, null, false);

		assertThat(props.accessTtl()).isEqualTo(Duration.ofMinutes(15));
		assertThat(props.refreshTtl()).isEqualTo(Duration.ofDays(30));
		assertThat(props.registration()).isEqualTo(Registration.INVITE);
	}

	@Test
	void refusesAMissingOrShortSecret() {
		assertThatThrownBy(() -> new AuthProperties(null, null, null, null, null, false))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("IHELPER_JWT_SECRET");
		assertThatThrownBy(() -> new AuthProperties("too-short", null, null, null, null, false))
			.isInstanceOf(IllegalStateException.class);
	}

}
