package com.topsoft.topsoftcrm_backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting για:
 *  - POST /api/auth/login     → 10 attempts / minute ανά IP (brute-force protection)
 *  - POST /api/webhook/**     → 60 requests / minute ανά IP
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Χωριστά buckets για login και webhooks
    private final Map<String, Bucket> loginBuckets   = new ConcurrentHashMap<>();
    private final Map<String, Bucket> webhookBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip   = getClientIp(request);

        if ("POST".equals(request.getMethod()) && path.startsWith("/api/auth/login")) {
            if (!loginBuckets.computeIfAbsent(ip, k -> loginBucket()).tryConsume(1)) {
                write429(response, "Πολλές αποτυχημένες προσπάθειες. Δοκιμάστε σε λίγο.");
                return;
            }
        } else if (path.startsWith("/api/webhook/")) {
            if (!webhookBuckets.computeIfAbsent(ip, k -> webhookBucket()).tryConsume(1)) {
                write429(response, "Rate limit exceeded.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    // 10 requests / minute ανά IP για login
    private Bucket loginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                .build();
    }

    // 60 requests / minute ανά IP για webhooks
    private Bucket webhookBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1))))
                .build();
    }

    private void write429(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private String getClientIp(HttpServletRequest request) {
        // Σε production πίσω από nginx, η πραγματική IP έρχεται στο header
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}