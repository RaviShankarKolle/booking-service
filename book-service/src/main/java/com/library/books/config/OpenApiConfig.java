package com.library.books.config;

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
    public OpenAPI bookServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Book Service API")
                        .description("""
                                Catalog and copy inventory. **JWT** is required for listing and for catalog CRUD \
                                (LIBRARIAN or ADMIN). **Reserve / issue / return** are open in this project so \
                                borrow-service can call them without forwarding a user token—protect these in production \
                                (e.g. mTLS, internal network, or service credential).""")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
