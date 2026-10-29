package com.ecommerce.tps.auth;

import com.ecommerce.tps.api.ApiDtos.AuthResponse;
import com.ecommerce.tps.api.ApiDtos.LoginRequest;
import com.ecommerce.tps.api.ApiDtos.RegisterRequest;
import com.ecommerce.tps.api.ApiDtos.UserResponse;
import com.ecommerce.tps.audit.AuditService;
import com.ecommerce.tps.common.ApiException;
import com.ecommerce.tps.security.TokenService;
import com.ecommerce.tps.user.UserAccount;
import com.ecommerce.tps.user.UserAccountRepository;
import com.ecommerce.tps.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {
	private final UserAccountRepository users;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final AuditService auditService;

	public AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder, TokenService tokenService,
			AuditService auditService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.auditService = auditService;
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		validatePassword(request.password());
		if (users.existsByEmailIgnoreCase(email)) {
			throw ApiException.conflict("Ya existe una cuenta con ese correo");
		}
		UserAccount user = users.save(new UserAccount(email, request.fullName().trim(),
				passwordEncoder.encode(request.password()), UserRole.CUSTOMER));
		auditService.recordAs(email, "REGISTER", "UserAccount", user.getId(), "Registro de cliente");
		return UserResponse.from(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		UserAccount user = users.findByEmailIgnoreCase(normalizeEmail(request.email()))
				.filter(UserAccount::isActive)
				.filter(account -> passwordEncoder.matches(request.password(), account.getPasswordHash()))
				.orElseThrow(() -> ApiException.unauthorized("Correo o contraseña incorrectos"));
		return new AuthResponse(tokenService.issue(user), "Bearer", tokenService.getExpirationSeconds(),
				UserResponse.from(user));
	}

	public static void validatePassword(String password) {
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw ApiException.badRequest("La contraseña no puede superar 72 bytes UTF-8");
		}
	}

	public static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
