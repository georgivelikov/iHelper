package com.ihelper.common;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the bundled React app from classpath:/static and falls back to index.html for client-side
 * routes, so deep links and reloads work. API, OpenAPI and Swagger paths are never rewritten.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

	private static final List<String> NOT_SPA = List.of("api/", "v3/", "swagger-ui");

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
			.addResourceLocations("classpath:/static/")
			.resourceChain(true)
			.addResolver(new PathResourceResolver() {
				@Override
				protected Resource getResource(String resourcePath, Resource location) throws IOException {
					Resource requested = location.createRelative(resourcePath);
					if (requested.isReadable() && !resourcePath.isEmpty() && !resourcePath.endsWith("/")) {
						return requested;
					}
					if (NOT_SPA.stream().anyMatch(resourcePath::startsWith) || resourcePath.contains(".")) {
						return null;
					}
					Resource index = new ClassPathResource("static/index.html");
					return index.isReadable() ? index : null;
				}
			});
	}

}
