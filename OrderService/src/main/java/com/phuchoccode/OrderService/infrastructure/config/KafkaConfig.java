package com.phuchoccode.OrderService.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.support.converter.ByteArrayJacksonJsonMessageConverter;

@Configuration
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory(
            ConsumerFactory<String, byte[]> consumerFactory) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, byte[]>();
        factory.setConsumerFactory(consumerFactory);

        // Dùng constructor mặc định — tự tìm JsonMapper từ classpath
        factory.setRecordMessageConverter(new ByteArrayJacksonJsonMessageConverter());
        return factory;
    }
}