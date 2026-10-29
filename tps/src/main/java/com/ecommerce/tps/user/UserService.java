package com.ecommerce.tps.user;

import com.ecommerce.tps.api.ApiDtos.UserResponse;
import com.ecommerce.tps.api.ApiDtos.UserUpdateRequest;
import com.ecommerce.tps.audit.AuditService;
import com.ecommerce.tps.common.ApiException;
import com.ecommerce.tps.auth.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
	private final UserAccountRepository users;
	private final PasswordEncoder passwordEncoder;
	private final AuditService auditService;

	public UserService(UserAccountRepository users, PasswordEncoder passwordEncoder, AuditService auditService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
	}

	@Transactional(readOnly = true)
	public Page<UserResponse> list(Pageable pageable) {
		return users.findAll(pageable).map(UserResponse::from);
	}

	@Transactional(readOnly = true)
	public UserResponse get(long id) {
		return UserResponse.from(find(id));
	}

	@Transactional
	public UserResponse update(long id, UserUpdateRequest request) {
		UserAccount user = find(id);
		user.setFullName(request.fullName().trim());
		user.setRole(request.role());
		user.setActive(request.active());
		if (request.password() != null && !request.password().isBlank()) {
			AuthService.validatePassword(request.password());
			user.setPasswordHash(passwordEncoder.encode(request.password()));
		}
		auditService.record("UPDATE", "UserAccount", user.getId(), "Actualización de usuario");
		return UserResponse.from(user);
	}

	@Transactional
	public void deactivate(long id) {
		UserAccount user = find(id);
		user.setActive(false);
		auditService.record("DEACTIVATE", "UserAccount", user.getId(), "Desactivación de usuario");
	}

	UserAccount find(long id) {
		return users.findById(id).orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
	}
}
