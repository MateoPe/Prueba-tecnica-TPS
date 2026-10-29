package com.ecommerce.tps.security;

import com.ecommerce.tps.user.UserAccount;
import com.ecommerce.tps.user.UserAccountRepository;
import com.ecommerce.tps.user.UserRole;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class TokenService {
	private static final String HEADER = Base64.getUrlEncoder().withoutPadding()
			.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
	private final ObjectMapper objectMapper;
	private final UserAccountRepository users;
	private final byte[] secret;
	private final long expirationSeconds;

	public TokenService(ObjectMapper objectMapper, UserAccountRepository users,
			@Value("${app.security.jwt-secret:local-development-secret-change-before-deploy-32chars}") String secret,
			@Value("${app.security.token-expiration-seconds:3600}") long expirationSeconds) {
		if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
		}
		if (expirationSeconds < 60) {
			throw new IllegalArgumentException("JWT expiration must be at least 60 seconds");
		}
		this.objectMapper = objectMapper;
		this.users = users;
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
		this.expirationSeconds = expirationSeconds;
	}

	public String issue(UserAccount user) {
		try {
			String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(
					Map.of("sub", user.getEmail(), "role", user.getRole().name(),
							"exp", Instant.now().getEpochSecond() + expirationSeconds)));
			String unsigned = HEADER + "." + payload;
			return unsigned + "." + sign(unsigned);
		} catch (JacksonException e) {
			throw new IllegalStateException("Could not create access token", e);
		}
	}

	public long getExpirationSeconds() { return expirationSeconds; }

	public Authentication authenticate(String token) {
		String[] parts = token.split("\\.", -1);
		if (parts.length != 3 || !HEADER.equals(parts[0])
				|| !MessageDigest.isEqual(sign(parts[0] + "." + parts[1])
						.getBytes(StandardCharsets.US_ASCII), parts[2].getBytes(StandardCharsets.US_ASCII))) {
			return null;
		}
		Map<?, ?> claims;
		try {
			claims = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
		} catch (JacksonException | IllegalArgumentException exception) {
			return null;
		}
		Object subject = claims.get("sub");
		Object expiresAt = claims.get("exp");
		Object roleClaim = claims.get("role");
		if (!(subject instanceof String email) || !(expiresAt instanceof Number expiration)
				|| expiration.longValue() <= Instant.now().getEpochSecond()
				|| !(roleClaim instanceof String roleText)) {
			return null;
		}
		UserRole role;
		try {
			role = UserRole.valueOf(roleText);
		} catch (IllegalArgumentException exception) {
			return null;
		}
		UserAccount user = users.findByEmailIgnoreCase(email).orElse(null);
		if (user == null || !user.isActive() || user.getRole() != role) {
			return null;
		}
		return new UsernamePasswordAuthenticationToken(user.getEmail(), null,
				java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret, "HmacSHA256"));
			return Base64.getUrlEncoder().withoutPadding()
					.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.US_ASCII)));
		} catch (java.security.GeneralSecurityException e) {
			throw new IllegalStateException("Could not sign access token", e);
		}
	}
}
