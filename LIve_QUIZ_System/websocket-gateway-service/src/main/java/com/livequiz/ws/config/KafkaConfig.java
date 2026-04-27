package com.livequiz.ws.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.livequiz.common.events.QuestionPublishedEvent;
import com.livequiz.common.events.QuizEndedEvent;
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

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ObjectMapper mapper) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        cfg.put(ProducerConfig.ACKS_CONFIG, "1");
        ProducerFactory<String, Object> pf =
                new DefaultKafkaProducerFactory<>(cfg, new StringSerializer(), new JsonSerializer<>(mapper));
        return new KafkaTemplate<>(pf);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QuestionPublishedEvent> questionListenerFactory(ObjectMapper mapper) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        // IMPORTANT: each gateway *instance* needs its own consumer group so
        // every node receives every QUESTION_PUBLISHED and can broadcast to
        // its local connections. We append the random instanceId for that.
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, "ws-gateway-" + java.util.UUID.randomUUID());
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        JsonDeserializer<QuestionPublishedEvent> json = new JsonDeserializer<>(QuestionPublishedEvent.class, mapper, false);
        json.addTrustedPackages("*");
        var cf = new DefaultKafkaConsumerFactory<>(cfg, new StringDeserializer(), json);
        var f = new ConcurrentKafkaListenerContainerFactory<String, QuestionPublishedEvent>();
        f.setConsumerFactory(cf);
        return f;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QuizEndedEvent> quizEndedListenerFactory(ObjectMapper mapper) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, "ws-gateway-ended-" + java.util.UUID.randomUUID());
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        JsonDeserializer<QuizEndedEvent> json = new JsonDeserializer<>(QuizEndedEvent.class, mapper, false);
        json.addTrustedPackages("*");
        var cf = new DefaultKafkaConsumerFactory<>(cfg, new StringDeserializer(), json);
        var f = new ConcurrentKafkaListenerContainerFactory<String, QuizEndedEvent>();
        f.setConsumerFactory(cf);
        return f;
    }
}

