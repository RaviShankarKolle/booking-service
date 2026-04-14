package com.library.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .description("""
                                Inbound REST hooks for sending emails (registration, password reset, borrow, overdue, fine). \
                                Called by user-service, borrow-service, and fine-payment-service. \
                                No JWT in this project—restrict by network or API gateway in production.""")
                        .version("v1"));
    }
}
