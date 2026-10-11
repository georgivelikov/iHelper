package com.ihelper.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.ihelper.TestcontainersConfiguration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = { "ihelper.auth.registration=invite", "ihelper.auth.invite-code=let-me-in-please" })
@Import(TestcontainersConfiguration.class)
class InviteRegistrationIT {

	private static final String PASSWORD = "correct-horse-battery";

	@LocalServerPort
	int port;

	@BeforeEach
	void configure() {
		RestAssured.port = port;
		RestAssured.baseURI = "http://localhost";
		RestAssured.basePath = "";
	}

	private static RequestSpecification json() {
		return given().contentType(ContentType.JSON).accept(ContentType.JSON);
	}

	@Test
	void needsTheInviteCode() {
		String name = "inv" + System.nanoTime();
		json().body(Map.of("username", name, "password", PASSWORD)).post("/api/v1/auth/register").then()
			.statusCode(403);
		json().body(Map.of("username", name, "password", PASSWORD, "inviteCode", "wrong"))
			.post("/api/v1/auth/register").then().statusCode(403);
		json().body(Map.of("username", name, "password", PASSWORD, "inviteCode", "let-me-in-please"))
			.post("/api/v1/auth/register").then().statusCode(201);
	}

}
