package com.msgetway.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

/**
 * Première barrière de sécurité : refuse dès l'entrée les requêtes sans token valide
 * vers les routes protégées. Les services revérifient le token (défense en profondeur).
 */
@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    /**
     * Routes accessibles sans être connecté (correspondance exacte, aucun joker).
     * Les métriques de la gateway sont sur un port séparé, non exposé (management.server.port).
     */
    private static final List<String> PUBLIC_PATHS = List.of("/auth/login", "/auth/register", "/auth/catalogue");

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final Key signKey;

    public JwtGatewayFilter(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret (JWT_SECRET) doit contenir au moins 32 caractères");
        }
        this.signKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())
                || PUBLIC_PATHS.stream().anyMatch(p -> matcher.match(p, path))) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ") || !isValid(header.substring(7))) {
            return unauthorized(exchange.getResponse());
        }
        return chain.filter(exchange);
    }

    private boolean isValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(signKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private static Mono<Void> unauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"status\":401,\"code\":-1,\"message\":\"Session expirée ou non authentifiée\",\"response\":\"Session expirée ou non authentifiée\"}"
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
