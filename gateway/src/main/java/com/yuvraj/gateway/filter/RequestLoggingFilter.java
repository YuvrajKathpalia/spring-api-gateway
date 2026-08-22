package com.yuvraj.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Cross-cutting access log applied to EVERY request (a {@link GlobalFilter}, unlike
 * the per-route JWT filter). Records method, URI, response status, and elapsed time.
 *
 * <p>Runs with the highest precedence so the timer brackets the whole chain.
 * This is also the natural home for a future correlation-id (trace-id) header.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startNanos = System.nanoTime();

        return chain.filter(exchange).doFinally(signalType -> {
            long tookMs = (System.nanoTime() - startNanos) / 1_000_000;
            var status = exchange.getResponse().getStatusCode();
            log.info("{} {} -> {} ({} ms)",
                    request.getMethod(),
                    request.getURI().getPath(),
                    status != null ? status.value() : "-",
                    tookMs);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
