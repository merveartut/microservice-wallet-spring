package com.wallet.wallet_service.messaging;

import com.wallet.common.exception.InvalidRequestException;
import com.wallet.user_service.dto.UserCreatedEvent;
import com.wallet.wallet_service.config.KafkaErrorHandlingConfig;
import com.wallet.wallet_service.entity.Account;
import com.wallet.wallet_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserConsumer {
    private final AccountRepository accountRepository;

    @KafkaListener(topics = KafkaErrorHandlingConfig.USER_CREATED_TOPIC, groupId = "wallet-group")
    @Transactional
    public void consume(UserCreatedEvent event) {
        if (event.userId() == null) {
            throw new InvalidRequestException("UserCreatedEvent içinde userId yok");
        }

        // Kafka at-least-once teslim eder: aynı event tekrar gelirse ikinci hesap açma
        if (accountRepository.existsByUserId(event.userId())) {
            log.info("userId={} için hesap zaten var, tekrar gelen event atlandı", event.userId());
            return;
        }

        Account account = new Account();
        account.setUserId(event.userId());
        account.setCurrency("TR");
        account.setBalance(BigDecimal.TEN);
        accountRepository.save(account);

        log.info("userId={} için otomatik hesap açıldı: {}", event.userId(), account.getAccountNumber());
    }
}