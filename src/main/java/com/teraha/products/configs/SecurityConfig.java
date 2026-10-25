package com.teraha.products.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
	@Bean 
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session ->
					session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
					.requestMatchers(
						"/v3/api-docs/**",
						"/v3/api-docs.yaml",
						"/swagger-ui.html",
						"/swagger-ui/**"
						).permitAll()
					.requestMatchers(HttpMethod.GET, "/products", "/products/**")
					.hasAnyRole("SELLER", "MANAGER", "SYSADMIN")
					.requestMatchers(HttpMethod.POST, "/products")
					.hasAnyRole("SELLER", "MANAGER", "SYSADMIN")
					.requestMatchers(HttpMethod.PATCH, "/products/*/inventory")
					.hasAnyRole("SELLER", "MANAGER", "SYSADMIN")
					.requestMatchers(
						HttpMethod.PUT, "/products/*")
					.hasRole("SYSADMIN")
					.requestMatchers(
						HttpMethod.PATCH, "/products/*")
					.hasRole("SYSADMIN")
					.requestMatchers(
						HttpMethod.DELETE, "/products/*")
					.hasRole("SYSADMIN")
					.anyRequest().authenticated())
					.oauth2ResourceServer(oauth2 ->
							oauth2.jwt(Customizer.withDefaults()))
					.build();
	}
}
