package com.livequiz.session.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.livequiz.session.model.PlayerSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Two templates because we read/write two different shapes:
 *   - PlayerSession (a JSON hash value)
 *   - Set<String> of playerIds per session (the room roster)
 * We pick a Jackson serialiser with JavaTimeModule so Instant fields survive
 * the round-trip without being coerced to epoch-millis strings.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, PlayerSession> sessionTemplate(RedisConnectionFactory cf) {
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var serializer = new Jackson2JsonRedisSerializer<>(mapper, PlayerSession.class);
        RedisTemplate<String, PlayerSession> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(serializer);
        t.setHashKeySerializer(new StringRedisSerializer());
        t.setHashValueSerializer(serializer);
        return t;
    }

    @Bean
    public RedisTemplate<String, String> stringTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        return t;
    }
}

