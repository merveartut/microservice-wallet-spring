package com.wallet.user_service.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.user_service.entity.OutboxEvent;
import com.wallet.user_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OutboxWriter {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Event'i outbox tablosuna yazar. MANDATORY: çağıran tarafın transaction'ı yoksa hata verir.
     * Outbox'ın tek amacı iş verisiyle aynı transaction'da yazılmak olduğu için bu bilinçli bir kısıttır.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String topic, String key, Object event) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setTopic(topic);
        outboxEvent.setMessageKey(key);
        outboxEvent.setEventType(event.getClass().getName());
        outboxEvent.setPayload(toJson(event));
        outboxEvent.setCreatedAt(Instant.now());
        outboxEventRepository.save(outboxEvent);
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Event JSON'a çevrilemedi: " + event.getClass().getName(), e);
        }
    }
}