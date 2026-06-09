// C:\...\wallet-service\src\main\java\com\wallet\wallet_service\messaging\UserConsumer.java
package com.wallet.wallet_service.messaging;

import com.wallet.wallet_service.dto.UserCreatedEvent;
import com.wallet.wallet_service.entity.Account;
import com.wallet.wallet_service.repository.AccountRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserConsumer {
    private final AccountRepository accountRepository;

    @KafkaListener(topics = "user-created-topic", groupId = "wallet-group")
    public void consume(UserCreatedEvent event) {
        System.out.println("Kafka'dan mesaj alındı, hesap oluşturuluyor: " + event.userId());

        Account account = new Account();
        account.setUserId(event.userId());
        account.setCurrency("TR");
        account.setAccountNumber("ACC-" + System.currentTimeMillis());
        account.setBalance(BigDecimal.TEN);

        accountRepository.save(account);
    }
}