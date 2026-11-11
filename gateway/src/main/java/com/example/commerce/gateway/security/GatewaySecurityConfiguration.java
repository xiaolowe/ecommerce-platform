package com.example.commerce.gateway.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                  ReactiveJwtDecoder jwtDecoder,
                                                  ReactiveJwtAuthenticationConverter jwtAuthenticationConverter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/media/**").permitAll()
                        .pathMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/v1/products/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/products/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers("/api/v1/media/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/v1/inventory/stocks").hasAuthority("ROLE_ADMIN")
                        .pathMatchers("/api/v1/inventory/reservations/**").hasAuthority("ROLE_ADMIN")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(@Value("${security.jwt.secret}") String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt ->
                Flux.fromIterable(jwt.getClaimAsStringList("roles"))
                        .map(SimpleGrantedAuthority::new));
        return converter;
    }
}
