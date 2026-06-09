package com.wallet.user_service.messaging;

import com.wallet.user_service.dto.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProducer {
    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    public void sendUserCreatedEvent(Long userId, String email) {
        UserCreatedEvent event = new UserCreatedEvent(userId, email);
        kafkaTemplate.send("user-created-topic", event);
    }
}