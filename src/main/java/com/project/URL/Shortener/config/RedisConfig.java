package com.project.URL.Shortener.config;

import com.project.URL.Shortener.entity.Url;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
public class RedisConfig {

    @Bean
    @Primary
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
    @Bean("clickCounterRedisTemplate")
    public RedisTemplate<String, Long> counterRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new org.springframework.data.redis.serializer.GenericToStringSerializer<>(Long.class));
        template.afterPropertiesSet();
        return template;
    }
}