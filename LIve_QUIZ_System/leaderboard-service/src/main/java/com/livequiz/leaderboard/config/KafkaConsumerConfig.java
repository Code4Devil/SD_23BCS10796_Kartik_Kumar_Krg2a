package com.livequiz.leaderboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.livequiz.common.events.AnswerSubmittedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumer config. `max.poll.records` is raised from the default 500 so a
 * single poll cycle can drain a burst of answers (everyone submitting in the
 * last second of the timer). Concurrency is set to 3 — one per Kafka
 * partition we'll provision for ANSWER_SUBMITTED in production.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}") private String bootstrap;

    @Bean
    public ObjectMapper kafkaMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public ConsumerFactory<String, AnswerSubmittedEvent> consumerFactory(ObjectMapper mapper) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, "leaderboard-service");
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        cfg.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);
        JsonDeserializer<AnswerSubmittedEvent> json =
                new JsonDeserializer<>(AnswerSubmittedEvent.class, mapper, false);
        json.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(cfg, new StringDeserializer(), json);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AnswerSubmittedEvent> listenerFactory(
            ConsumerFactory<String, AnswerSubmittedEvent> cf) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, AnswerSubmittedEvent>();
        f.setConsumerFactory(cf);
        f.setConcurrency(3);
        return f;
    }
}

