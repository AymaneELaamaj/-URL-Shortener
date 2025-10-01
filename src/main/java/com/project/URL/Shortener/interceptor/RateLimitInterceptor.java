package com.project.URL.Shortener.interceptor;

import com.project.URL.Shortener.service.RedisRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisRateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        String shortCode = extractShortCode(request);

        // Vérifier tous les rate limits
        boolean allowed = rateLimitService.checkAllRateLimits(ip, shortCode);

        if (!allowed) {
            log.warn("🚨 Rate limit exceeded - IP: {}, ShortCode: {}", ip, shortCode);

            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"Rate limit exceeded\", \"message\": \"Too many requests, please try again later.\"}"
            );

            return false; // Bloquer la requête
        }

        return true; // Autoriser la requête
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String extractShortCode(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Extraire le shortCode de /api/url/{shortCode}
        if (path.startsWith("/api/url/")) {
            String[] parts = path.split("/");
            if (parts.length >= 4) {
                return parts[3];
            }
        }
        return "unknown";
    }
}