package com.project.URL.Shortener.config;

import com.project.URL.Shortener.entity.Url;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // 🔹 Configuration pour éviter les conflits avec Spring Boot 3.5+

    // Shard 1 - Le marquer comme PRIMARY pour l'auto-configuration
    @Bean
    @Primary  // ⬅️ TRÈS IMPORTANT
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
        return new LettuceConnectionFactory(config);
    }

    // Shard 2 - Avec un nom spécifique
    @Bean
    public RedisConnectionFactory shard2ConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6380);
        return new LettuceConnectionFactory(config);
    }

    // Shard 3 - Avec un nom spécifique
    @Bean
    public RedisConnectionFactory shard3ConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6381);
        return new LettuceConnectionFactory(config);
    }

    // Shard 4 - Avec un nom spécifique
    @Bean
    public RedisConnectionFactory shard4ConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6382);
        return new LettuceConnectionFactory(config);
    }

    // 🔹 AJOUTER CE BEAN - redisTemplate par défaut (pour Spring)
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(
            @Qualifier("redisConnectionFactory") RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        template.afterPropertiesSet();
        return template;
    }

    // RedisTemplates avec qualifiers explicites
    @Bean("redisShard1")
    public RedisTemplate<String, Url> redisShard1(
            @Qualifier("redisConnectionFactory") RedisConnectionFactory connectionFactory) {
        return createTemplate(connectionFactory);
    }

    @Bean("redisShard2")
    public RedisTemplate<String, Url> redisShard2(
            @Qualifier("shard2ConnectionFactory") RedisConnectionFactory connectionFactory) {
        return createTemplate(connectionFactory);
    }

    @Bean("redisShard3")
    public RedisTemplate<String, Url> redisShard3(
            @Qualifier("shard3ConnectionFactory") RedisConnectionFactory connectionFactory) {
        return createTemplate(connectionFactory);
    }

    @Bean("redisShard4")
    public RedisTemplate<String, Url> redisShard4(
            @Qualifier("shard4ConnectionFactory") RedisConnectionFactory connectionFactory) {
        return createTemplate(connectionFactory);
    }

    // RedisTemplate pour compteur - utiliser le primary
    @Bean("clickCounterRedisTemplate")
    public RedisTemplate<String, Long> counterRedisTemplate(
            @Qualifier("redisConnectionFactory") RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new org.springframework.data.redis.serializer.GenericToStringSerializer<>(Long.class));
        template.afterPropertiesSet();
        return template;
    }

    private RedisTemplate<String, Url> createTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Url> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Url.class));
        template.afterPropertiesSet();
        return template;
    }
}