package com.library.users.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI userServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .description("""
                                Library user accounts: registration, login, password reset, profile, and internal \
                                read APIs for borrow/fine/notification services. \
                                Use **Authentication** endpoints without a token; obtain a JWT from **login**, then \
                                click **Authorize** and paste `Bearer <token>` or only the token (depending on UI). \
                                **Internal** endpoints are for service-to-service calls (no JWT in this project).""")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT from POST /api/v1/auth/login (same secret as other services in dev).")));
    }
}
