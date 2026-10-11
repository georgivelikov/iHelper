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

	@LocalServerPort
	int port;

	@BeforeEach
	void configureRestAssured() {
		RestAssured.port = port;
		RestAssured.baseURI = "http://localhost";
		RestAssured.basePath = "";
	}

	protected static RequestSpecification api() {
		return given().contentType(ContentType.JSON).accept(ContentType.JSON);
	}

	protected static String unique(String prefix) {
		return prefix + "-" + UUID.randomUUID();
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
