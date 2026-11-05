package com.wallet.wallet_service.config;

import com.wallet.common.exception.InvalidRequestException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class KafkaErrorHandlingConfig {

    public static final String USER_CREATED_TOPIC = "user-created-topic";
    public static final String USER_CREATED_DLT = USER_CREATED_TOPIC + ".DLT";

    /** İşlenemeyen mesajların yazılacağı topic. Uygulama açılışında yoksa oluşturulur. */
    @Bean
    public NewTopic userCreatedDltTopic() {
        return TopicBuilder.name(USER_CREATED_DLT).partitions(1).replicas(1).build();
    }

    /**
     * DLT'ye yazmak için producer.
     * Normal mesajlar JSON olarak yazılır. Çözümlenemeyen (bozuk) mesajların elimizde yalnızca
     * ham byte'ları olduğu için onlar olduğu gibi (byte[]) yazılır.
     */
    @Bean
    public KafkaTemplate<String, Object> deadLetterKafkaTemplate(KafkaProperties kafkaProperties) {
        Map<Class<?>, Serializer<?>> valueSerializers = new LinkedHashMap<>();
        valueSerializers.put(byte[].class, new ByteArraySerializer());
        valueSerializers.put(Object.class, new JsonSerializer<>());

        DefaultKafkaProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers()),
                new StringSerializer(),
                new DelegatingByTypeSerializer(valueSerializers, true));
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Listener'dan exception çıkarsa: 3 kez tekrar dene (1 sn, 2 sn, 4 sn), yine olmazsa DLT'ye yaz.
     * Spring Boot bu bean'i @KafkaListener container'larına otomatik bağlar.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> deadLetterKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate);

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        // Geçersiz veri tekrar denemekle düzelmez: doğrudan DLT
        errorHandler.addNotRetryableExceptions(InvalidRequestException.class);
        return errorHandler;
    }
}