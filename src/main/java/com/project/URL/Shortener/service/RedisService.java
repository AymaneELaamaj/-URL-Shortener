package com.project.URL.Shortener.service;

import com.project.URL.Shortener.cache.RedisShardManager;
import com.project.URL.Shortener.entity.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis Service avec sharding + observabilité
 */
@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);
    private final RedisShardManager shardManager;

    public RedisService(RedisShardManager shardManager) {
        this.shardManager = shardManager;
    }

    /**
     * Set URL in cache avec TTL de 24h
     *
     * Pourquoi 24h ?
     * ✅ Balance entre freshness et cache efficiency
     * ✅ Si URL change, max 24h avant sync
     */
    public void set(String shortCode, Url url) {
        try {
            shardManager.getShard(shortCode)
                    .opsForValue()
                    .set(shortCode, url, 24, TimeUnit.HOURS);

            logger.debug("✅ Cache SET: {} → {}", shortCode,
                    shardManager.getShardDebugInfo(shortCode));
        } catch (Exception e) {
            logger.error("❌ Cache SET failed for {}: {}", shortCode, e.getMessage());
        }
    }

    /**
     * Get URL from cache
     */
    public Url get(String shortCode) {
        try {
            Url url = shardManager.getShard(shortCode)
                    .opsForValue()
                    .get(shortCode);

            if (url != null) {
                logger.debug("✅ Cache HIT: {}", shortCode);
            } else {
                logger.debug("❌ Cache MISS: {}", shortCode);
            }

            return url;
        } catch (Exception e) {
            logger.error("❌ Cache GET failed for {}: {}", shortCode, e.getMessage());
            return null; // Fallback to DB
        }
    }

    /**
     * Delete from cache (invalidation)
     */
    public void delete(String shortCode) {
        try {
            shardManager.getShard(shortCode)
                    .delete(shortCode);
            logger.debug("✅ Cache DELETE: {}", shortCode);
        } catch (Exception e) {
            logger.error("❌ Cache DELETE failed for {}: {}", shortCode, e.getMessage());
        }
    }
}