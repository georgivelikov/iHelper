package com.ihelper.auth;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ihelper.AbstractIT;

class AuthApiIT extends AbstractIT {

	private static String name() {
		return unique("u").substring(0, 24);
	}

	@Test
	void registerCreatesAnAccountWithStarterCategoriesAndReturnsTokens() {
		String username = name();

		var response = anonymous().body(Map.of("username", username, "password", PASSWORD))
			.post("/api/v1/auth/register")
			.then()
			.statusCode(201)
			.body("tokenType", equalTo("Bearer"))
			.body("expiresIn", equalTo(900))
			.body("user.username", equalTo(username))
			.body("accessToken", notNullValue())
			.body("refreshToken", nullValue()) // web transport: the refresh token is a cookie only
			.extract();

		String setCookie = response.header("Set-Cookie");
		org.assertj.core.api.Assertions.assertThat(setCookie)
			.contains("ihelper_refresh=", "HttpOnly", "SameSite=Strict", "Path=/api/v1/auth");

		given(response.path("accessToken"))
			.get("/api/v1/categories")
			.then()
			.statusCode(200)
			.body("name", contains("Personal", "Ideas", "Work"))
			.body("$", hasSize(3));
	}

	private static io.restassured.specification.RequestSpecification given(String token) {
		return anonymous().header("Authorization", "Bearer " + token);
	}

	@Test
	void registerValidatesUsernameAndPassword() {
		anonymous().body(Map.of("username", "ab", "password", PASSWORD))
			.post("/api/v1/auth/register")
			.then()
			.statusCode(400)
			.body("errors.field", hasItems("username"));
		anonymous().body(Map.of("username", name(), "password", "short"))
			.post("/api/v1/auth/register")
			.then()
			.statusCode(400)
			.body("errors.field", hasItems("password"));
	}

	@Test
	void usernamesAreUniqueIgnoringCase() {
		String username = name();
		register(username);

		anonymous().body(Map.of("username", username.toUpperCase(), "password", PASSWORD))
			.post("/api/v1/auth/register")
			.then()
			.statusCode(409)
			.contentType(containsString("application/problem+json"));
	}

	@Test
	void loginWorksAndBadCredentialsAreIndistinguishable() {
		String username = name();
		register(username);

		anonymous().body(Map.of("username", username.toUpperCase(), "password", PASSWORD))
			.post("/api/v1/auth/login")
			.then()
			.statusCode(200)
			.body("user.username", equalTo(username));

		String wrongPassword = anonymous().body(Map.of("username", username, "password", "wrong-password-1"))
			.post("/api/v1/auth/login")
			.then()
			.statusCode(401)
			.header("WWW-Authenticate", "Bearer")
			.extract()
			.path("detail");
		String unknownUser = anonymous().body(Map.of("username", name(), "password", "wrong-password-1"))
			.post("/api/v1/auth/login")
			.then()
			.statusCode(401)
			.extract()
			.path("detail");
		org.assertj.core.api.Assertions.assertThat(wrongPassword).isEqualTo(unknownUser);
	}

	@Test
	void repeatedFailuresAreRateLimited() {
		String username = name();
		register(username);

		for (int i = 0; i < 8; i++) {
			anonymous().body(Map.of("username", username, "password", "wrong-password-1"))
				.post("/api/v1/auth/login")
				.then()
				.statusCode(401);
		}

		// Even the right password is refused while the window is full.
		anonymous().body(Map.of("username", username, "password", PASSWORD))
			.post("/api/v1/auth/login")
			.then()
			.statusCode(429)
			.header("Retry-After", notNullValue());
	}

	@Test
	void apiRequiresAValidAccessToken() {
		anonymous().get("/api/v1/categories")
			.then()
			.statusCode(401)
			.contentType(containsString("application/problem+json"))
			.header("WWW-Authenticate", "Bearer");
		anonymous().header("Authorization", "Bearer not-a-jwt").get("/api/v1/tasks").then().statusCode(401);

		String token = currentAccessToken();
		String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
		anonymous().header("Authorization", "Bearer " + tampered).get("/api/v1/tasks").then().statusCode(401);
	}

	@Test
	void meReturnsTheSignedInUser() {
		String username = name();
		Session session = register(username);

		as(session).get("/api/v1/me")
			.then()
			.statusCode(200)
			.body("username", equalTo(username))
			.body("id", equalTo(session.userId()));
	}

	@Test
	void refreshRotatesTheTokenAndReuseEndsEverySession() {
		Session session = register(name());
		String first = session.refreshToken();

		String second = anonymous().header("X-Token-Transport", "body")
			.body(Map.of("refreshToken", first))
			.post("/api/v1/auth/refresh")
			.then()
			.statusCode(200)
			.body("accessToken", notNullValue())
			.extract()
			.path("refreshToken");
		org.assertj.core.api.Assertions.assertThat(second).isNotEqualTo(first);

		// Replaying the old token is treated as theft: it fails and revokes the new one too.
		anonymous().body(Map.of("refreshToken", first)).post("/api/v1/auth/refresh").then().statusCode(401);
		anonymous().body(Map.of("refreshToken", second)).post("/api/v1/auth/refresh").then().statusCode(401);
	}

	@Test
	void refreshWorksFromTheCookieAndLogoutRevokesIt() {
		String username = name();
		register(username);
		String cookie = anonymous().body(Map.of("username", username, "password", PASSWORD))
			.post("/api/v1/auth/login")
			.then()
			.statusCode(200)
			.extract()
			.cookie("ihelper_refresh");

		String rotated = anonymous().cookie("ihelper_refresh", cookie)
			.post("/api/v1/auth/refresh")
			.then()
			.statusCode(200)
			.extract()
			.cookie("ihelper_refresh");

		anonymous().cookie("ihelper_refresh", rotated).post("/api/v1/auth/logout").then().statusCode(204);
		anonymous().cookie("ihelper_refresh", rotated).post("/api/v1/auth/refresh").then().statusCode(401);
		// logout is idempotent
		anonymous().cookie("ihelper_refresh", rotated).post("/api/v1/auth/logout").then().statusCode(204);
	}

	@Test
	void refreshWithoutATokenIsUnauthorized() {
		anonymous().post("/api/v1/auth/refresh").then().statusCode(401);
		anonymous().body(Map.of("refreshToken", "nope")).post("/api/v1/auth/refresh").then().statusCode(401);
	}

	@Test
	void passwordsAreNeverReturnedOrStoredInClear() {
		String username = name();
		var body = anonymous().body(Map.of("username", username, "password", PASSWORD))
			.post("/api/v1/auth/register")
			.then()
			.body(not(containsString(PASSWORD)))
			.extract()
			.asString();
		org.assertj.core.api.Assertions.assertThat(body).doesNotContain("passwordHash");
	}

}
