package com.ihelper;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * The hosted configuration: prod profile, secrets from the environment. The database comes from
 * Testcontainers (its connection details override the placeholder DB_URL).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "spring.profiles.active=prod", "DB_URL=jdbc:postgresql://unused:5432/unused", "DB_USER=unused",
				"DB_PASSWORD=unused", "IHELPER_JWT_SECRET=prod-profile-test-secret-0123456789abcdef",
				"IHELPER_REGISTRATION=invite", "IHELPER_INVITE_CODE=prod-invite" })
@Import(TestcontainersConfiguration.class)
class ProdProfileIT {

	@LocalServerPort
	int port;

	@BeforeEach
	void configure() {
		RestAssured.port = port;
		RestAssured.baseURI = "http://localhost";
		RestAssured.basePath = "";
	}

	@Test
	void healthIsPublicAndRevealsNothingElse() {
		given().get("/actuator/health").then().statusCode(200).body("status", equalTo("UP"))
			.body(not(containsString("components")));
		given().get("/actuator/env").then().statusCode(404);
		given().get("/actuator/beans").then().statusCode(404);
	}

	@Test
	void apiDocsAndSwaggerAreOffByDefault() {
		given().get("/v3/api-docs").then().statusCode(404);
		given().get("/swagger-ui/index.html").then().statusCode(404);
	}

	@Test
	void apiNeedsAuthenticationAndRegistrationNeedsTheInviteCode() {
		given().get("/api/v1/tasks").then().statusCode(401);
		given().contentType(ContentType.JSON)
			.body(Map.of("username", "prod" + System.nanoTime(), "password", "correct-horse-battery"))
			.post("/api/v1/auth/register").then().statusCode(403);
	}

	@Test
	void refreshCookieIsSecureAndResponsesCarrySecurityHeaders() {
		var response = given().contentType(ContentType.JSON)
			.body(Map.of("username", "prod" + System.nanoTime(), "password", "correct-horse-battery", "inviteCode",
					"prod-invite"))
			.post("/api/v1/auth/register").then().statusCode(201).extract();

		org.assertj.core.api.Assertions.assertThat(response.header("Set-Cookie")).contains("Secure", "HttpOnly",
				"SameSite=Strict");
		org.assertj.core.api.Assertions.assertThat(response.header("X-Content-Type-Options")).isEqualTo("nosniff");
		org.assertj.core.api.Assertions.assertThat(response.header("Content-Security-Policy"))
			.contains("default-src 'self'", "frame-ancestors 'none'");
		org.assertj.core.api.Assertions.assertThat(response.header("Referrer-Policy")).isEqualTo("no-referrer");
		org.assertj.core.api.Assertions.assertThat(response.header("Cache-Control")).contains("no-store");
	}

}
