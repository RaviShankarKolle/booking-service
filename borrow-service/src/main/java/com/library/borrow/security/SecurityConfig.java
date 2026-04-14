package com.library.borrow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.borrow.common.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private final TokenAuthenticationFilter tokenAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(TokenAuthenticationFilter tokenAuthenticationFilter, ObjectMapper objectMapper) {
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    ApiResponse.error("UNAUTHORIZED", "Missing or invalid JWT.", Map.of())));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    ApiResponse.error("ACCESS_DENIED",
                                            "USER role is required to create or list borrows; LIBRARIAN or ADMIN for allocate/return.",
                                            Map.of())));
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/borrows/internal/overdue")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/borrows").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/borrows/users/**").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/borrows/*/allocate").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/borrows/*/return").hasAnyRole("LIBRARIAN", "ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
