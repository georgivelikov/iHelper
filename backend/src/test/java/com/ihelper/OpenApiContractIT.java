package com.ihelper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Pins the public API surface. The mobile app is built against this document, so any change must be
 * deliberate: review the diff, then refresh the snapshot with
 * {@code mvn verify -Dopenapi.update=true} and commit it.
 */
class OpenApiContractIT extends AbstractIT {

	private static final Path SNAPSHOT = Path.of("src/test/resources/openapi-snapshot.json");

	private static final ObjectMapper PRETTY = JsonMapper.builder()
		.enable(SerializationFeature.INDENT_OUTPUT)
		.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
		.build();

	@Test
	void apiDocsMatchTheCommittedSnapshot() throws Exception {
		String raw = RestAssured.get("/v3/api-docs").then().statusCode(200).extract().asString();
		JsonNode tree = PRETTY.readTree(raw);
		// the server URL contains the random test port
		((ObjectNode) tree).remove("servers");
		String actual = PRETTY.writeValueAsString(PRETTY.treeToValue(tree, Object.class)) + "\n";

		if (Boolean.getBoolean("openapi.update") || Files.notExists(SNAPSHOT)) {
			Files.createDirectories(SNAPSHOT.getParent());
			Files.writeString(SNAPSHOT, actual, StandardCharsets.UTF_8);
		}

		String expected = Files.readString(SNAPSHOT, StandardCharsets.UTF_8).replace("
", "
");
		assertThat(actual)
			.as("OpenAPI contract changed. If intended, rerun with -Dopenapi.update=true and commit the snapshot.")
			.isEqualTo(expected);
	}

	@Test
	void swaggerUiIsServed() {
		RestAssured.given().redirects().follow(true).get("/swagger-ui").then().statusCode(200);
	}

	@Test
	void everyEndpointIsDocumented() {
		var paths = RestAssured.get("/v3/api-docs").then().statusCode(200).extract().jsonPath()
			.getMap("paths").keySet();

		assertThat(paths).contains("/api/v1/categories", "/api/v1/categories/{id}", "/api/v1/tasks",
				"/api/v1/tasks/{id}", "/api/v1/sync");
	}

}
