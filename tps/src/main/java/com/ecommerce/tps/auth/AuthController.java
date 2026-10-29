package com.ecommerce.tps.auth;

import com.ecommerce.tps.api.ApiDtos.AuthResponse;
import com.ecommerce.tps.api.ApiDtos.LoginRequest;
import com.ecommerce.tps.api.ApiDtos.RegisterRequest;
import com.ecommerce.tps.api.ApiDtos.UserResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro de clientes e inicio de sesión para obtener JWT")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) { this.authService = authService; }

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Registrar cliente", description = "Crea una cuenta de cliente con rol CUSTOMER. El registro es público y el correo debe ser único.")
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	@Operation(summary = "Iniciar sesión", description = "Valida las credenciales y devuelve un access token JWT Bearer.")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}
}
