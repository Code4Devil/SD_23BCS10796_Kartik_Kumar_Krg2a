package com.livequiz.anticheat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.livequiz.common.events.AnswerSubmittedEvent;
import com.livequiz.common.events.BehaviorEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}") private String bootstrap;

    @Bean public ObjectMapper mapper() { return new ObjectMapper().registerModule(new JavaTimeModule()); }

    // ---------- Producer ----------
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ObjectMapper mapper) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        cfg.put(ProducerConfig.ACKS_CONFIG, "all");
        ProducerFactory<String, Object> pf =
                new DefaultKafkaProducerFactory<>(cfg, new StringSerializer(), new JsonSerializer<>(mapper));
        return new KafkaTemplate<>(pf);
    }

    // ---------- Consumer: BehaviorEvent ----------
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BehaviorEvent> behaviorListenerFactory(ObjectMapper mapper) {
        return listener(BehaviorEvent.class, "anti-cheat-behavior", mapper);
    }

    // ---------- Consumer: AnswerSubmittedEvent ----------
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AnswerSubmittedEvent> answerListenerFactory(ObjectMapper mapper) {
        return listener(AnswerSubmittedEvent.class, "anti-cheat-answers", mapper);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> listener(
            Class<T> type, String groupId, ObjectMapper mapper) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        JsonDeserializer<T> json = new JsonDeserializer<>(type, mapper, false);
        json.addTrustedPackages("*");
        ConsumerFactory<String, T> cf = new DefaultKafkaConsumerFactory<>(cfg, new StringDeserializer(), json);
        var f = new ConcurrentKafkaListenerContainerFactory<String, T>();
        f.setConsumerFactory(cf);
        return f;
    }
}

