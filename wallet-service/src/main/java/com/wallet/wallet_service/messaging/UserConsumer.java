// C:\...\wallet-service\src\main\java\com\wallet\wallet_service\messaging\UserConsumer.java
package com.wallet.wallet_service.messaging;

import com.wallet.user_service.dto.UserCreatedEvent;
import com.wallet.wallet_service.entity.Account;
import com.wallet.wallet_service.repository.AccountRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserConsumer {
    private final AccountRepository accountRepository;

    @KafkaListener(topics = "user-created-topic", groupId = "wallet-group")
    @Transactional
    public void consume(UserCreatedEvent event, Acknowledgment ack) {
        try {
            System.out.println("Kafka'dan mesaj alındı, hesap oluşturuluyor: " + event.userId());

            Account account = new Account();
            account.setUserId(event.userId());
            account.setCurrency("TR");
            account.setAccountNumber("ACC-" + System.currentTimeMillis());
            account.setBalance(BigDecimal.TEN);

            accountRepository.save(account);

            // İşlem bitti, mesajı onaylıyoruz
            ack.acknowledge();
        } catch (Exception e) {
            System.err.println("Hata oluştu, mesaj reddediliyor: " + e.getMessage());
            // Burada hata olursa ack.acknowledge() çağırmıyoruz, mesaj kuyrukta kalır
        }
    }
}