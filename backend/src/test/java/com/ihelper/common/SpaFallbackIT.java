package com.ihelper.common;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import com.ihelper.AbstractIT;

/** Uses the stub src/test/resources/static/index.html in place of the bundled frontend. */
class SpaFallbackIT extends AbstractIT {

	@Test
	void servesTheAppAtRootAndForClientSideRoutes() {
		given().get("/").then().statusCode(200).body(containsString("test shell"));
		given().get("/categories/work").then().statusCode(200).body(containsString("test shell"));
	}

	@Test
	void unknownApiAndAssetPathsStay404InsteadOfFallingBackToTheApp() {
		api().get("/api/v1/nope").then().statusCode(404);
		given().get("/api/v1/nope").then().statusCode(401); // never the HTML app
		given().get("/assets/missing.js").then().statusCode(404);
	}

	@Test
	void apiAndOpenApiDocsAreNotShadowed() {
		api().get("/api/v1/categories").then().statusCode(200);
		given().get("/v3/api-docs").then().statusCode(200).body(containsString("openapi"));
	}

}
