package com.project.URL.Shortener.config;

import com.project.URL.Shortener.entity.Url;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Url> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Url> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());

    // Use JSON serializer
    Jackson2JsonRedisSerializer<Url> serializer = new Jackson2JsonRedisSerializer<>(Url.class);
    template.setValueSerializer(serializer);
    template.afterPropertiesSet();
    return template;
    }
}