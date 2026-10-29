package com.ecommerce.tps.security;

import com.ecommerce.tps.user.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
	private final UserAccountRepository users;

	public DatabaseUserDetailsService(UserAccountRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return users.findByEmailIgnoreCase(username)
				.map(account -> User.withUsername(account.getEmail())
						.password(account.getPasswordHash())
						.authorities("ROLE_" + account.getRole().name())
						.disabled(!account.isActive())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}
}
