package com.livequiz.gateway.config;

import com.livequiz.gateway.security.JwtAuthenticationFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * All routing and cross-cutting concerns for inbound traffic live here.
 *
 * Design decisions:
 *   - Routes are defined in code (not YAML) so the JWT filter can be attached
 *     per-route without relying on filter name lookups.
 *   - Rate limiting uses Redis (replenishRate=20/s, burstCapacity=40) keyed by
 *     client IP. In a real prod setup we'd key by authenticated user ID.
 *   - The WebSocket route does NOT strip prefixes; STOMP handshake paths must
 *     match exactly on the downstream gateway service.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder b, JwtAuthenticationFilter jwt) {
        return b.routes()
                .route("quiz-orchestrator", r -> r.path("/api/quiz/**")
                        .filters(f -> f.filter(jwt.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(ipKeyResolver())))
                        .uri("http://quiz-orchestrator:8081"))
                .route("player-session", r -> r.path("/api/session/**")
                        .filters(f -> f.filter(jwt.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://player-session:8082"))
                .route("answer-processing", r -> r.path("/api/answer/**")
                        .filters(f -> f.filter(jwt.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://answer-processing:8083"))
                .route("leaderboard", r -> r.path("/api/leaderboard/**")
                        .uri("http://leaderboard:8084"))
                .route("anti-cheat", r -> r.path("/api/anticheat/**")
                        .filters(f -> f.filter(jwt.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://anti-cheat:8085"))
                // STOMP/SockJS handshake + long-lived frames go straight through.
                .route("websocket", r -> r.path("/ws/**")
                        .uri("http://websocket-gateway:8086"))
                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // 20 req/s sustained, 40 req burst — tuned for answer submissions
        // during simultaneous countdown timers (10K players pressing a
        // button within ~500 ms of each other).
        return new RedisRateLimiter(20, 40);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress())
                .defaultIfEmpty("anonymous");
    }
}

