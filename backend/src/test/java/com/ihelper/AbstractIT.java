package com.ihelper;

import static io.restassured.RestAssured.given;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/** Full HTTP-to-Postgres tests. All IT classes share one Spring context and one container. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIT {

	public static final String PASSWORD = "correct-horse-battery";

	@LocalServerPort
	int port;

	/** Access token of the user created for the running test; every test starts as a fresh user. */
	private static volatile String accessToken;

	@BeforeEach
	void configureRestAssured() {
		RestAssured.port = port;
		RestAssured.baseURI = "http://localhost";
		RestAssured.basePath = "";
		accessToken = register(unique("user").substring(0, 30)).accessToken();
	}

	/** A signed-in session: the access token and the user it belongs to. */
	public record Session(String accessToken, String refreshToken, String userId, String username) {
	}

	protected static Session register(String username) {
		var response = anonymous()
			.header("X-Token-Transport", "body")
			.body(Map.of("username", username, "password", PASSWORD))
			.post("/api/v1/auth/register")
			.then()
			.statusCode(201)
			.extract();
		return new Session(response.path("accessToken"), response.path("refreshToken"), response.path("user.id"),
				username);
	}

	/** Requests as the current test user. */
	protected static RequestSpecification api() {
		return anonymous().header("Authorization", "Bearer " + accessToken);
	}

	/** Requests as a given session, e.g. a second user. */
	protected static RequestSpecification as(Session session) {
		return anonymous().header("Authorization", "Bearer " + session.accessToken());
	}

	protected static RequestSpecification anonymous() {
		return given().contentType(ContentType.JSON).accept(ContentType.JSON);
	}

	protected static String unique(String prefix) {
		return prefix + "-" + UUID.randomUUID();
	}

	protected static String currentAccessToken() {
		return accessToken;
	}

	/** Creates a category through the API and returns its id. */
	protected static String createCategory(String name) {
		return api().body(Map.of("name", name)).post("/api/v1/categories").then().statusCode(201).extract()
			.path("id");
	}

	/** Creates a task through the API and returns its id. */
	protected static String createTask(Map<String, Object> body) {
		return api().body(body).post("/api/v1/tasks").then().statusCode(201).extract().path("id");
	}

}
