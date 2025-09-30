package com.project.URL.Shortener.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration des 4 shards Redis
 */
@Configuration
public class RedisShardConfig {

    @Value("${redis.shard1.host:localhost}")
    private String shard1Host;
    @Value("${redis.shard1.port:6379}")
    private int shard1Port;

    @Value("${redis.shard2.host:localhost}")
    private String shard2Host;
    @Value("${redis.shard2.port:6380}")
    private int shard2Port;

    @Value("${redis.shard3.host:localhost}")
    private String shard3Host;
    @Value("${redis.shard3.port:6381}")
    private int shard3Port;

    @Value("${redis.shard4.host:localhost}")
    private String shard4Host;
    @Value("${redis.shard4.port:6382}")
    private int shard4Port;

    @Bean
    public List<StringRedisTemplate> redisTemplates() {
        List<StringRedisTemplate> templates = new ArrayList<>();
        templates.add(createTemplate(shard1Host, shard1Port));
        templates.add(createTemplate(shard2Host, shard2Port));
        templates.add(createTemplate(shard3Host, shard3Port));
        templates.add(createTemplate(shard4Host, shard4Port));
        return templates;
    }

    private StringRedisTemplate createTemplate(String host, int port) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        return new StringRedisTemplate(factory);
    }
}

