package com.ecommerce.tps.security;

import com.ecommerce.tps.api.ApiDtos.ApiError;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(java.util.List.of("http://localhost:4200", "http://localhost:5173"));
		configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokenService, ObjectMapper mapper)
			throws Exception {
		http.csrf(csrf -> csrf.disable())
				.cors(cors -> {})
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) ->
						writeError(mapper, response, HttpServletResponse.SC_UNAUTHORIZED,
								"Unauthorized", "Autenticación requerida", request.getRequestURI()))
						.accessDeniedHandler((request, response, exception) ->
								writeError(mapper, response, HttpServletResponse.SC_FORBIDDEN,
										"Forbidden", "No tienes permisos para esta operación",
										request.getRequestURI())))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/error").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
						.requestMatchers("/api/users/**", "/api/inventory/**", "/api/reports/**", "/api/audit/**")
							.hasRole("ADMIN")
						.requestMatchers("/api/products/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.addFilterBefore(new BearerTokenFilter(tokenService), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	private static void writeError(ObjectMapper mapper, HttpServletResponse response, int status,
			String error, String message, String path) throws java.io.IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		mapper.writeValue(response.getOutputStream(),
				new ApiError(Instant.now(), status, error, message, path));
	}
}
