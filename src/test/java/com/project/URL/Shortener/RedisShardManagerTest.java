package com.project.URL.Shortener;

import com.project.URL.Shortener.cache.RedisShardManager;
import com.project.URL.Shortener.entity.Url;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire du RedisShardManager avec mocks
 */
public class RedisShardManagerTest {

    @Test
    public void testKeyDistribution() {
        // Créer des mocks pour les 4 shards
        RedisTemplate<String, Url> shard1 = Mockito.mock(RedisTemplate.class);
        RedisTemplate<String, Url> shard2 = Mockito.mock(RedisTemplate.class);
        RedisTemplate<String, Url> shard3 = Mockito.mock(RedisTemplate.class);
        RedisTemplate<String, Url> shard4 = Mockito.mock(RedisTemplate.class);

        // Instanciation du manager avec mocks
        RedisShardManager shardManager = new RedisShardManager(shard1, shard2, shard3, shard4);

        int numKeys = 1000;
        Map<String, Integer> shardCounts = new HashMap<>();

        // Générer 1000 short codes
        for (int i = 0; i < numKeys; i++) {
            String shortCode = "key" + i;
            String shardId = shardManager.getShard(shortCode).toString();
            shardCounts.put(shardId, shardCounts.getOrDefault(shardId, 0) + 1);
        }

        // Vérifier distribution uniforme (±10%)
        int expectedPerShard = numKeys / 4;
        for (Integer count : shardCounts.values()) {
            assertThat(count).isBetween(
                    (int) (expectedPerShard * 0.9),
                    (int) (expectedPerShard * 1.1)
            );
        }

        System.out.println("📊 Distribution des clés : " + shardCounts);
    }
}
