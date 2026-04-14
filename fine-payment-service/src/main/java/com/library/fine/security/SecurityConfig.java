package com.library.fine.security;



import com.fasterxml.jackson.databind.ObjectMapper;

import com.library.fine.common.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.http.MediaType;

import org.springframework.security.config.Customizer;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;



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

                                    ApiResponse.error("ACCESS_DENIED", "Insufficient role for requested fine operation.", Map.of())));

                        }))

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/v1/fines/**").hasAnyRole("LIBRARIAN", "ADMIN")

                        .anyRequest().authenticated())

                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }

}

