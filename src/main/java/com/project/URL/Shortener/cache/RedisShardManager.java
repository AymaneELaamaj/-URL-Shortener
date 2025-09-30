package com.project.URL.Shortener.cache;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Gestion des shards Redis avec Consistent Hashing
 */
@Component
public class RedisShardManager {

    private static final int VIRTUAL_NODES = 150;
    private final TreeMap<Long, StringRedisTemplate> ring = new TreeMap<>();

    public RedisShardManager(List<StringRedisTemplate> redisTemplates) {
        for (int i = 0; i < redisTemplates.size(); i++) {
            String shardName = "shard-" + i;
            StringRedisTemplate template = redisTemplates.get(i);

            for (int j = 0; j < VIRTUAL_NODES; j++) {
                String virtualNodeName = shardName + "#" + j;
                long hash = hash(virtualNodeName);
                ring.put(hash, template);
            }
        }
    }

    public StringRedisTemplate getShard(String key) {
        if (ring.isEmpty()) throw new IllegalStateException("No Redis shards available");

        long hash = hash(key);
        Map.Entry<Long, StringRedisTemplate> entry = ring.ceilingEntry(hash);

        return (entry != null) ? entry.getValue() : ring.firstEntry().getValue();
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            long h = 0;
            for (int i = 0; i < 8; i++) {
                h = (h << 8) | (digest[i] & 0xFF);
            }
            return h;
        } catch (Exception e) {
            throw new RuntimeException("Hash error", e);
        }
    }
}

