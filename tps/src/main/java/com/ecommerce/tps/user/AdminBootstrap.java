package com.ecommerce.tps.user;

import com.ecommerce.tps.auth.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {
	private final UserAccountRepository users;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String password;

	public AdminBootstrap(UserAccountRepository users, PasswordEncoder passwordEncoder,
			@Value("${app.bootstrap-admin.email:}") String email,
			@Value("${app.bootstrap-admin.password:}") String password) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(String... args) {
		if (email.isBlank() != password.isBlank()) {
			throw new IllegalStateException("Set both APP_BOOTSTRAP_ADMIN_EMAIL and APP_BOOTSTRAP_ADMIN_PASSWORD");
		}
		if (!email.isBlank() && !users.existsByEmailIgnoreCase(email.trim())) {
			if (password.length() < 12) {
				throw new IllegalStateException("Bootstrap admin password must contain at least 12 characters");
			}
			users.save(new UserAccount(AuthService.normalizeEmail(email), "Administrator",
					passwordEncoder.encode(password), UserRole.ADMIN));
		}
	}
}
