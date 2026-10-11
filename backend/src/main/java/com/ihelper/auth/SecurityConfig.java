package com.ihelper.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Stateless bearer-token security. Everything under /api needs a valid access token except /api/v1/auth;
 * the bundled frontend, health and (when enabled) API docs are public.
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

	private static final String CSP = "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; "
			+ "frame-ancestors 'none'; base-uri 'self'; form-action 'self'";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		AuthenticationEntryPoint unauthorized = (request, response, ex) -> problem(response, HttpStatus.UNAUTHORIZED,
				"Unauthorized", "Authentication is required");
		AccessDeniedHandler forbidden = (request, response, ex) -> problem(response, HttpStatus.FORBIDDEN, "Forbidden",
				"You do not have access to this resource");
		http
			.csrf(AbstractHttpConfigurer::disable) // stateless bearer API; the refresh cookie is SameSite=Strict
			.cors(Customizer.withDefaults())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(a -> a
				.requestMatchers("/api/v1/auth/**").permitAll()
				.requestMatchers("/api/**").authenticated()
				.anyRequest().permitAll())
			.oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults())
				.authenticationEntryPoint(unauthorized)
				.accessDeniedHandler(forbidden))
			.exceptionHandling(e -> e.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
			.headers(h -> h
				.referrerPolicy(r -> r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
				// Swagger UI needs inline scripts, so it is the one place the policy is not applied.
				.addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
						request -> !request.getRequestURI().startsWith("/swagger-ui"),
						new StaticHeadersWriter("Content-Security-Policy", CSP))));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JwtEncoder jwtEncoder(AuthProperties properties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
	}

	@Bean
	JwtDecoder jwtDecoder(AuthProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(properties))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.ISSUER));
		return decoder;
	}

	private static SecretKey secretKey(AuthProperties properties) {
		return new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	private static void problem(HttpServletResponse response, HttpStatus status, String title, String detail)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		if (status == HttpStatus.UNAUTHORIZED) {
			response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		}
		response.getWriter()
			.write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status.value()
					+ ",\"detail\":\"" + detail + "\"}");
	}

}
