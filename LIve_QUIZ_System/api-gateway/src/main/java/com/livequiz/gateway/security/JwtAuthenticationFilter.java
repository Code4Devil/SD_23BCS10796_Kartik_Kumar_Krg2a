package com.livequiz.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Reactive JWT gateway filter. Verifies the bearer token and forwards the
 * authenticated playerId to downstream services through the X-Player-Id
 * header, so the microservices themselves don't need JWT libraries.
 * This is the "trust the gateway" pattern used by most mature API gateways.
 */
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final SecretKey signingKey;

    public JwtAuthenticationFilter(@Value("${livequiz.jwt.secret:change-me-please-change-me-please-32b}") String secret) {
        super(Config.class);
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public GatewayFilter apply(Config cfg) {
        return (exchange, chain) -> {
            String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return unauthorized(exchange);
            }
            try {
                Claims claims = Jwts.parser().verifyWith(signingKey).build()
                        .parseSignedClaims(auth.substring(7)).getPayload();
                ServerHttpRequest mutated = exchange.getRequest().mutate()
                        .header("X-Player-Id", claims.getSubject())
                        .header("X-Player-Role", String.valueOf(claims.get("role", String.class)))
                        .build();
                return chain.filter(exchange.mutate().request(mutated).build());
            } catch (Exception e) {
                return unauthorized(exchange);
            }
        };
    }

    private Mono<Void> unauthorized(org.springframework.web.server.ServerWebExchange ex) {
        ex.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return ex.getResponse().setComplete();
    }

    public static class Config {}
}

