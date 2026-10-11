package com.ihelper.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Every operation needs a bearer token unless it opts out with an empty {@code @SecurityRequirements}. */
@Configuration
public class OpenApiConfig {

	static final String SCHEME = "bearerAuth";

	@Bean
	OpenAPI openApi() {
		return new OpenAPI()
			.info(new Info().title("iHelper API").version("v1"))
			.components(new Components().addSecuritySchemes(SCHEME,
					new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(SCHEME));
	}

}
