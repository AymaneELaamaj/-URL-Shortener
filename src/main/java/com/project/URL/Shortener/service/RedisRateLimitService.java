package com.project.URL.Shortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j

public class RedisRateLimitService {

    private final RedisTemplate<String, String> redisTemplate;
    public RedisRateLimitService(@Qualifier("rateLimitRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Configurations
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final long WINDOW_MS = 60_000; // 1 minute

    public boolean isAllowed(String key) {
        String redisKey = "ratelimit:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;

        try {
            // 1. Supprimer les vieilles entrées
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            // 2. Compter les requêtes récentes
            Long count = redisTemplate.opsForZSet().zCard(redisKey);

            if (count == null || count < MAX_REQUESTS_PER_MINUTE) {
                // 3. Ajouter la requête actuelle
                redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);

                // 4. Expiration automatique
                redisTemplate.expire(redisKey, 2, TimeUnit.MINUTES);

                log.debug("✅ RateLimit ALLOWED - Key: {}, Count: {}", key, count);
                return true;
            }

            log.debug("❌ RateLimit BLOCKED - Key: {}, Count: {}", key, count);
            return false;

        } catch (Exception e) {
            log.error("❌ RateLimit ERROR - Key: {}, Error: {}", key, e.getMessage());
            return true; // Fail-open
        }
    }

    public boolean isIpAllowed(String ip) {
        return isAllowed("ip:" + ip);
    }

    public boolean isShortCodeAllowed(String shortCode) {
        return isAllowed("code:" + shortCode);
    }

    // ⭐ MÉTHODE MANQUANTE - AJOUTE ÇA !
    public boolean checkAllRateLimits(String ip, String shortCode) {
        // Vérifier rate limiting par IP
        if (!isIpAllowed(ip)) {
            log.warn("🚨 Rate limit exceeded for IP: {}", ip);
            return false;
        }

        // Vérifier rate limiting par shortCode
        if (!isShortCodeAllowed(shortCode)) {
            log.warn("🚨 Rate limit exceeded for ShortCode: {}", shortCode);
            return false;
        }

        log.debug("✅ All rate limits passed for IP: {}, ShortCode: {}", ip, shortCode);
        return true;
    }
}