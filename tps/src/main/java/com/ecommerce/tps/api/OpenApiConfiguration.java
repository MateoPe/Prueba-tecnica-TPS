package com.ecommerce.tps.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "TPS E-commerce API",
				version = "1.0.0",
				description = """
						API REST para gestionar usuarios, productos, inventario y órdenes de compra.
						Los endpoints protegidos usan JWT Bearer. Registra un cliente o inicia sesión
						para obtener un token; el administrador local se configura con variables de entorno.
						""",
				contact = @Contact(name = "Equipo TPS")),
		servers = {
				@Server(url = "http://localhost:8080", description = "Entorno local")
		})
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT",
		in = SecuritySchemeIn.HEADER,
		description = "Pega solo el accessToken devuelto por POST /api/auth/login.")
public class OpenApiConfiguration {
}
