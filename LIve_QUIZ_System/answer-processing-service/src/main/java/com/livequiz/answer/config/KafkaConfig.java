package com.livequiz.answer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.livequiz.common.events.QuestionPublishedEvent;
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

    @Bean
    public ObjectMapper kafkaMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // ---------- Producer ----------
    @Bean
    public ProducerFactory<String, Object> producerFactory(ObjectMapper mapper) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        cfg.put(ProducerConfig.ACKS_CONFIG, "all");
        cfg.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(cfg, new StringSerializer(), new JsonSerializer<>(mapper));
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>(pf);
    }

    // ---------- Consumer for QUESTION_PUBLISHED ----------
    @Bean
    public ConsumerFactory<String, QuestionPublishedEvent> questionConsumerFactory(ObjectMapper mapper) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, "answer-processing-question-cache");
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        // Use an error-tolerant JsonDeserializer so poison-pill messages can't
        // stall the consumer thread; they're logged and skipped.
        JsonDeserializer<QuestionPublishedEvent> json = new JsonDeserializer<>(QuestionPublishedEvent.class, mapper, false);
        json.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(cfg, new StringDeserializer(), json);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QuestionPublishedEvent> questionListenerFactory(
            ConsumerFactory<String, QuestionPublishedEvent> cf) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, QuestionPublishedEvent>();
        f.setConsumerFactory(cf);
        return f;
    }
}

