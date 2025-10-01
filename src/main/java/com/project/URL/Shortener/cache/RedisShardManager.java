package com.project.URL.Shortener.cache;

import com.project.URL.Shortener.entity.Url;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Redis Shard Manager avec Consistent Hashing
 *
 * ✅ Résout le problème : si on ajoute/enlève un shard,
 *    seulement ~25% des clés migrent (vs 75% avec modulo)
 */
@Component
public class RedisShardManager {

    private static final int VIRTUAL_NODES = 150; // Pour distribution uniforme

    // Le "ring" : TreeMap pour clockwise lookup en O(log n)
    private final TreeMap<Long, RedisTemplate<String, Url>> ring = new TreeMap<>();

    // Pour health checks
    private final Map<String, RedisTemplate<String, Url>> shardMap = new HashMap<>();

    public RedisShardManager(
            @Qualifier("redisShard1") RedisTemplate<String, Url> shard1,
            @Qualifier("redisShard2") RedisTemplate<String, Url> shard2,
            @Qualifier("redisShard3") RedisTemplate<String, Url> shard3,
            @Qualifier("redisShard4") RedisTemplate<String, Url> shard4
    ) {
        List<RedisTemplate<String, Url>> shards = List.of(shard1, shard2, shard3, shard4);

        // Placer chaque shard sur le ring avec virtual nodes
        for (int i = 0; i < shards.size(); i++) {
            String shardName = "shard-" + i;
            RedisTemplate<String, Url> shard = shards.get(i);

            shardMap.put(shardName, shard);

            // Créer 150 virtual nodes par shard
            for (int j = 0; j < VIRTUAL_NODES; j++) {
                String virtualNodeName = shardName + "#" + j;
                long hash = hash(virtualNodeName);
                ring.put(hash, shard);
            }
        }

        System.out.println("✅ RedisShardManager initialized with " + shards.size() +
                " shards and " + ring.size() + " virtual nodes");
    }

    /**
     * Trouver le shard pour une clé donnée
     *
     * Algorithm : Consistent Hashing
     * 1. Hash la clé
     * 2. Clockwise lookup : trouve le premier virtual node >= hash
     * 3. Retourne le shard correspondant
     */
    public RedisTemplate<String, Url> getShard(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("No shards available");
        }

        long hash = hash(key);

        // Chercher le premier node >= hash (clockwise)
        Map.Entry<Long, RedisTemplate<String, Url>> entry = ring.ceilingEntry(hash);

        // Si aucun trouvé, wrap around (prendre le premier du ring)
        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    /**
     * Hash function : MD5
     *
     * Pourquoi MD5 ?
     * ✅ Rapide (important pour des millions de req/sec)
     * ✅ Bonne distribution uniforme
     * ✅ Disponible nativement en Java
     *
     * Pourquoi pas SHA-256 ? Trop lent (2x plus lent que MD5)
     * Pourquoi pas hashCode() ? Mauvaise distribution
     */
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));

            // Convertir les 8 premiers bytes en long (64 bits)
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            return hash;
        } catch (Exception e) {
            throw new RuntimeException("Hash generation failed", e);
        }
    }

    /**
     * Health check : vérifier si tous les shards sont alive
     */
    public Map<String, Boolean> getShardHealth() {
        Map<String, Boolean> health = new HashMap<>();
        for (Map.Entry<String, RedisTemplate<String, Url>> entry : shardMap.entrySet()) {
            try {
                entry.getValue().hasKey("health-check");
                health.put(entry.getKey(), true);
            } catch (Exception e) {
                health.put(entry.getKey(), false);
            }
        }
        return health;
    }

    /**
     * Pour debugging : voir quelle clé va sur quel shard
     */
    public String getShardDebugInfo(String key) {
        RedisTemplate<String, Url> shard = getShard(key);
        int shardIndex = -1;

        // Trouver l'index du shard
        for (Map.Entry<String, RedisTemplate<String, Url>> entry : shardMap.entrySet()) {
            if (entry.getValue() == shard) {
                shardIndex = Integer.parseInt(entry.getKey().replace("shard-", ""));
                break;
            }
        }

        return String.format("Key '%s' → Shard %d (hash: %d)", key, shardIndex, hash(key));
    }
}