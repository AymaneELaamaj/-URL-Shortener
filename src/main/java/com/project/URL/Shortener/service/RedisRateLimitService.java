package com.project.URL.Shortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisRateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    // Configurations simplifiées
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final long WINDOW_MS = 60_000; // 1 minute en millisecondes

    /**
     * Version SIMPLIFIÉE pour débutant
     */
    public boolean isAllowed(String key) {
        String redisKey = "ratelimit:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;

        try {
            // 1. Supprimer les vieilles entrées (hors fenêtre)
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            // 2. Compter le nombre de requêtes récentes
            Long count = redisTemplate.opsForZSet().zCard(redisKey);

            if (count == null || count < MAX_REQUESTS_PER_MINUTE) {
                // 3. Ajouter la requête actuelle
                redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);

                // 4. Définir expiration automatique
                redisTemplate.expire(redisKey, 2, TimeUnit.MINUTES);

                log.debug("✅ RateLimit ALLOWED - Key: {}, Count: {}", key, count);
                return true;
            }

            log.debug("❌ RateLimit BLOCKED - Key: {}, Count: {}", key, count);
            return false;

        } catch (Exception e) {
            log.error("❌ RateLimit ERROR - Key: {}, Error: {}", key, e.getMessage());
            return true; // En cas d'erreur, on autorise (fail-open)
        }
    }

    /**
     * Vérifier rate limiting par IP
     */
    public boolean isIpAllowed(String ip) {
        return isAllowed("ip:" + ip);
    }

    /**
     * Vérifier rate limiting par shortCode
     */
    public boolean isShortCodeAllowed(String shortCode) {
        return isAllowed("code:" + shortCode);
    }
}