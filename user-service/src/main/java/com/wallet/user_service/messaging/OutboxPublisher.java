package com.wallet.user_service.messaging;

import com.wallet.user_service.entity.OutboxEvent;
import com.wallet.user_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private static final int BATCH_SIZE = 100;
    private static final long SEND_TIMEOUT_SECONDS = 10;
    private static final Duration RETENTION = Duration.ofDays(7);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Gönderilmemiş mesajları sırayla Kafka'ya gönderir ve gönderilenleri işaretler.
     * Bir mesaj gönderilemezse durur; kalanlar (sıra bozulmasın diye) bir sonraki turda denenir.
     */
    @Scheduled(fixedDelayString = "${outbox.publish-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outboxEventRepository.lockNextBatch(BATCH_SIZE);
        for (OutboxEvent event : batch) {
            try {
                send(event);
                event.setPublishedAt(Instant.now());
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("Outbox mesajı gönderilemedi, sonraki turda tekrar denenecek. id={}, topic={}, hata={}",
                        event.getId(), event.getTopic(), e.getMessage());
                break;
            }
        }
        // Metot bitince transaction commit edilir: gönderilenlerin published_at değeri kaydedilir
    }

    private void send(OutboxEvent event) throws Exception {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(event.getTopic(), event.getMessageKey(), event.getPayload());
        // Consumer'ın JsonDeserializer'ı hangi sınıfa çevireceğini bu header'dan öğrenir
        record.headers().add("__TypeId__", event.getEventType().getBytes(StandardCharsets.UTF_8));
        // get(): Kafka "aldım" diyene kadar bekle. Ancak ondan sonra gönderildi olarak işaretlenir.
        kafkaTemplate.send(record).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /** Gönderilmiş eski kayıtları temizler; tablo sonsuza kadar büyümesin. */
    @Scheduled(fixedDelayString = "${outbox.cleanup-interval-ms:3600000}")
    @Transactional
    public void deleteOldPublished() {
        int deleted = outboxEventRepository.deletePublishedBefore(Instant.now().minus(RETENTION));
        if (deleted > 0) {
            log.info("{} adet gönderilmiş outbox kaydı silindi", deleted);
        }
    }
}