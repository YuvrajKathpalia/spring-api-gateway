package com.yuvraj.gateway.filter;

import com.yuvraj.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Per-route filter (referenced as {@code JwtAuth} in application.yml) that protects
 * routes by requiring a valid JWT.
 *
 * <p>On every protected request it:
 * <ol>
 *   <li>reads the {@code Authorization: Bearer <token>} header (401 if absent),</li>
 *   <li>verifies the signature and expiry with the shared secret (401 if invalid),</li>
 *   <li>extracts {@code userId} / {@code email} / {@code roles} from the claims,</li>
 *   <li>injects them as {@code X-User-*} headers so downstream services can trust
 *       them without ever parsing a JWT.</li>
 * </ol>
 *
 * <p>Extensibility seam: a Redis session-liveness check would slot in right after
 * signature verification (see the marked spot below) before forwarding.
 */
@Component
public class JwtAuthGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGatewayFilterFactory.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey verificationKey;

    public JwtAuthGatewayFilterFactory(JwtProperties properties) {
        super(Config.class);
        this.verificationKey = new SecretKeySpec(
                properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing or malformed Authorization header");
            }

            String token = authHeader.substring(BEARER_PREFIX.length());
            Claims claims;
            try {
                claims = Jwts.parser()
                        .verifyWith(verificationKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (JwtException | IllegalArgumentException ex) {
                // Covers bad signature, expired, malformed, etc.
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            }

            // --- Future extensibility seam ---------------------------------
            // A Redis session-liveness check (stateful revocation over stateless
            // JWT) would go here, before forwarding. Left out by design for now.
            // ---------------------------------------------------------------

            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            String roles = claims.get("roles", String.class);

            // Replace the request with one carrying trusted identity headers.
            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", email == null ? "" : email)
                    .header("X-User-Roles", roles == null ? "" : roles)
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        };
    }

    /** Writes a JSON error body and short-circuits the filter chain. */
    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        log.warn("JWT auth rejected [{}]: {}", status.value(), message);
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        String body = String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                status.value(), status.getReasonPhrase(), message);
        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Per-route configuration. Empty today, but present so routes can later pass
     * options (e.g. required roles) without changing the filter's signature.
     */
    public static class Config {
    }
}
