package com.ecommerce.tps.user;

import com.ecommerce.tps.api.ApiDtos.UserResponse;
import com.ecommerce.tps.api.ApiDtos.UserUpdateRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuarios", description = "Administración de cuentas de clientes")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
	private final UserService userService;
	public UserController(UserService userService) { this.userService = userService; }

	@GetMapping
	@Operation(summary = "Listar usuarios", description = "Devuelve las cuentas paginadas. Requiere rol ADMIN.")
	public Page<UserResponse> list(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return userService.list(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar usuario", description = "Obtiene el perfil y estado de una cuenta. Requiere rol ADMIN.")
	public UserResponse get(@PathVariable long id) { return userService.get(id); }

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar usuario", description = "Actualiza nombre, rol y estado; la contraseña es opcional. Requiere rol ADMIN.")
	public UserResponse update(@PathVariable long id, @Valid @RequestBody UserUpdateRequest request) {
		return userService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Desactivar usuario", description = "Desactiva la cuenta conservando su historial. Requiere rol ADMIN.")
	public void deactivate(@PathVariable long id) { userService.deactivate(id); }
}
