package com.livequiz.ws.config;

import com.livequiz.ws.bridge.LeaderboardSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Wires the Redis Pub/Sub channel published by the Leaderboard Service into
 * our STOMP broker. Every gateway node subscribes to the same channel, so
 * whichever node a player is connected to will receive the update.
 */
@Configuration
public class RedisListenerConfig {

    @Bean
    public RedisMessageListenerContainer listenerContainer(RedisConnectionFactory cf,
                                                           LeaderboardSubscriber subscriber) {
        var c = new RedisMessageListenerContainer();
        c.setConnectionFactory(cf);
        // LeaderboardSubscriber already implements MessageListener, so we
        // register it directly — no adapter / reflection indirection.
        c.addMessageListener(subscriber, new PatternTopic("leaderboard.updates"));
        return c;
    }
}

