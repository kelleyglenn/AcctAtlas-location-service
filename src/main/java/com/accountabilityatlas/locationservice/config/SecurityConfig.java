package com.accountabilityatlas.locationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final LenientJwtAuthenticationFilter lenientJwtAuthenticationFilter;

  public SecurityConfig(LenientJwtAuthenticationFilter lenientJwtAuthenticationFilter) {
    this.lenientJwtAuthenticationFilter = lenientJwtAuthenticationFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/locations/**")
                    .permitAll()
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(
                    (request, response, authException) -> {
                      response.setStatus(HttpStatus.UNAUTHORIZED.value());
                      response.setContentType("application/json");
                      response
                          .getWriter()
                          .write(
                              "{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}");
                    }))
        .addFilterBefore(
            lenientJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
