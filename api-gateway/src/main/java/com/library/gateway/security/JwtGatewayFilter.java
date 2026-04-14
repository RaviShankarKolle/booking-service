package com.library.gateway.security;

import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/actuator/health",
            "/swagger-ui",
            "/v3/api-docs"
    );

    /** Gateway-only Swagger prefixes (see application.yml routes under /docs/{service-id}/...). */
    private static final List<String> DOC_SERVICE_IDS = List.of(
            "user-service",
            "book-service",
            "borrow-service",
            "fine-payment-service",
            "notification-service"
    );

    private final JwtTokenValidator tokenValidator;

    public JwtGatewayFilter(JwtTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublicPath(path) || isGatewayDocsPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = tokenValidator.validateAndGetClaims(token);
            String rolesHeader = rolesClaimToHeader(claims.get("roles"));
            ServerHttpRequest mutated = exchange.getRequest()
                    .mutate()
                    .header("X-Auth-User-Id", claims.getSubject())
                    .header("X-Auth-Roles", rolesHeader)
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isGatewayDocsPath(String path) {
        for (String serviceId : DOC_SERVICE_IDS) {
            String base = "/docs/" + serviceId + "/";
            if (path.startsWith(base + "swagger-ui") || path.startsWith(base + "v3/api-docs")) {
                return true;
            }
        }
        return false;
    }

    /**
     * JWT "roles" may be a Collection, String, or other type depending on serializer;
     * downstream services expect a simple comma-separated list (e.g. USER,ADMIN).
     */
    private static String rolesClaimToHeader(Object rolesClaim) {
        if (rolesClaim == null) {
            return "";
        }
        if (rolesClaim instanceof Collection<?> col) {
            return col.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
        }
        return rolesClaim.toString().trim();
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
