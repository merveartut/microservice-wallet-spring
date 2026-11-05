# Yapılacaklar — Bilinen Sorunlar

Kod incelemesi ve çalışan sistem üzerinde yapılan testlerle bulunan sorunların listesidir. Öncelik sırasına göre dizilmiştir. Bir madde çözüldüğünde ☐ işaretini ☑ yapın.

**Son güncelleme:** 2026-11-05. Sorunların ayrıntılı açıklaması: [Teknik Doküman — Bölüm 12](teknik-dokuman.md#12-bilinen-sorunlar). Yapılan değişiklikler: [Değişiklik Kaydı](degisiklikler.md).

**Öncelik:** 🔴 Kritik · 🟠 Yüksek · 🟡 Orta · 🟢 Düşük

## Açık

| Durum | # | Öncelik | Sorun | Konum | Önerilen çözüm |
|---|---|---|---|---|---|
| ☐ | 35 | 🔴 Kritik | `.env` `.gitignore`'da olmasına rağmen git'te takip ediliyor (`729e7ae` ile eklenmiş); `JWT_SECRET` GitHub'da görünüyor. Anahtarı bilen biri istediği kullanıcı/ADMIN adına token üretebilir | [.env](../.env) | `git rm --cached .env` + yeni anahtar üretip servisleri yeniden başlatmak (eski anahtar git geçmişinde kalır) |
| ☐ | 34 | 🟠 Yüksek | Farklı para birimindeki hesaplar arasında transfer kur dönüşümü yapmıyor: TRY → USD 2,50 gönderilince USD hesabı 2,50 dolar artıyor (frontend testinde bulundu) | [AccountService.java](../wallet-service/src/main/java/com/wallet/wallet_service/service/AccountService.java) `transferMoney` | Para birimleri farklıysa 422 `CURRENCY_MISMATCH` ile reddet; ya da kur servisiyle dönüşüm |
| ☐ | 14 | 🟡 Orta | Postgres şifreleri compose dosyasında açık metin | [docker-compose.yml:27](../docker-compose.yml#L27), [docker-compose.yml:39](../docker-compose.yml#L39) | `.env`'e taşı (`JWT_SECRET` gibi) |
| ☐ | 15 | 🟡 Orta | `depends_on` healthcheck beklemiyor; servisler DB/Kafka hazır olmadan açılabiliyor | [docker-compose.yml:83](../docker-compose.yml#L83), [docker-compose.yml:100](../docker-compose.yml#L100) | `healthcheck` + `condition: service_healthy` |
| ☐ | 16 | 🟡 Orta | Kafka `trusted.packages=*`; her sınıfın deserialize edilmesine izin veriyor | [application.properties:27](../wallet-service/src/main/resources/application.properties#L27) (wallet) | Sadece `com.wallet.user_service.dto`'ya izin ver |
| ☐ | 33 | 🟡 Orta | DLT'de mesaj biriktiğinde alarm yok; DLT'deki mesajlar elle ana topic'e gönderiliyor | [KafkaErrorHandlingConfig.java](../wallet-service/src/main/java/com/wallet/wallet_service/config/KafkaErrorHandlingConfig.java) | DLT lag izleme + yeniden işleme aracı ([Kafka Kılavuzu 9.5](kafka-kilavuzu.md#95-dltdeki-mesaj-nasıl-yeniden-işlenir)) |
| ☐ | 30 | 🟡 Orta | user-service'e ulaşılamadığında hesap açma 500 dönüyor; gateway hataları (503) Problem Details formatında değil | [AccountService.java:24](../wallet-service/src/main/java/com/wallet/wallet_service/service/AccountService.java#L24) | `FeignException` → 503 `SERVICE_UNAVAILABLE`; gateway'e WebFlux hata yöneticisi |
| ☐ | 17 | 🟢 Düşük | Zookeeper tanımlı ama kullanılmıyor (Kafka KRaft modunda) | [docker-compose.yml:45](../docker-compose.yml#L45) | Servisi compose'tan kaldır |
| ☐ | 18 | 🟢 Düşük | Kafka ve Zookeeper imajları `latest`; sürüm sabit değil | [docker-compose.yml:53](../docker-compose.yml#L53) | Belirli bir sürüm etiketi kullan |
| ☐ | 19 | 🟢 Düşük | `currency` serbest metin; otomatik hesaplarda `TR` yazıyor (doğru kod `TRY`) | [UserConsumer.java:29](../wallet-service/src/main/java/com/wallet/wallet_service/messaging/UserConsumer.java#L29) | Enum veya ISO 4217 doğrulaması |
| ☐ | 20 | 🟢 Düşük | Başarılı cevaplar tutarsız: giriş, hesap açma, para yatırma ve transfer düz metin dönüyor | [UserController.java:47](../user-service/src/main/java/com/wallet/user_service/controller/UserController.java#L47), [AccountController.java](../wallet-service/src/main/java/com/wallet/wallet_service/controller/AccountController.java) | JSON DTO'lar (örn. `{"token": "..."}`) |
| ☐ | 22 | 🟢 Düşük | `show-sql=true` ve Security/Gateway DEBUG logları açık | [application.properties:25](../user-service/src/main/resources/application.properties#L25) (user), [application.properties:22](../api-gateway/src/main/resources/application.properties#L22) (gateway) | `dev`/`prod` profillerine ayır |
| ☐ | 23 | 🟢 Düşük | Gateway'de merkezi JWT doğrulaması, rate limit ve CORS yok | [SecurityConfig.java:17](../api-gateway/src/main/java/com/wallet/api_gateway/config/SecurityConfig.java#L17) (gateway) | Gateway'e JWT filtresi ve `RequestRateLimiter` ekle |
| ☐ | 24 | 🟢 Düşük | Projede hiç test yok | — | JUnit birim testleri + Testcontainers ile entegrasyon testleri |
| ☐ | 29 | 🟢 Düşük | Spring'in ürettiği bazı hata mesajları İngilizce ("Failed to read request", "Required parameter 'amount' is not present.") | [GlobalExceptionHandler.java](../common-security/src/main/java/com/wallet/common/exception/GlobalExceptionHandler.java) | `MessageSource` ile `problemDetail.*` mesajlarını Türkçeleştir |
| ☐ | 31 | 🟢 Düşük | Transfer'de olmayan kaynak hesap 404, başkasına ait kaynak hesap 403 dönüyor; hesap numaralarının varlığı tahmin edilebilir | [AccountService.java:51](../wallet-service/src/main/java/com/wallet/wallet_service/service/AccountService.java#L51) | Kaynak hesap için iki durumda da 403 dön |
| ☐ | 32 | 🟢 Düşük | `JwtAuthenticationFilter`'da kullanılmayan `java.util.ArrayList` import'u | [JwtAuthenticationFilter.java:14](../common-security/src/main/java/com/wallet/common/security/JwtAuthenticationFilter.java#L14) | Import'u sil |

## Önerilen Sıra

1. **#35:** Gizli anahtar herkese açık; ilk iş bu.
2. **#34 ve #31:** İkisi de `transferMoney` içinde, birkaç satırlık değişiklik.
3. **#30:** Hata altyapısının kapsamadığı son boşluk (servisler arası çağrı hataları).
4. **#14, #15, #17, #18:** Hepsi `docker-compose.yml` içinde, birlikte yapılabilir.
5. **#24:** Diğer düzeltmelerin bozulmadığından emin olmak için testler.
6. Diğerleri tablodaki öncelik sırasıyla.

Satır numaraları bu listenin güncellendiği andaki koda göredir; kod değiştikçe kayabilir.

## Tamamlananlar

| Durum | # | Sorun | Çözüm | Commit |
|---|---|---|---|---|
| ☑ | — | `JWT_SECRET` container'lara aktarılmıyordu; servisler açılışta çöküyordu | `docker-compose.yml`'e eklendi | `2f5f10d` |
| ☑ | — | README'de olmayan bir endpoint yazıyordu | `/api/account/user/{userId}` olarak düzeltildi | `8aa90ac` |
| ☑ | — | API cevapları şifre hash'ini içeriyordu | `@JsonProperty(access = WRITE_ONLY)` | `8c9d2b0` |
| ☑ | 1 | Transfer'de kaynak hesap sahipliği kontrol edilmiyordu | Serviste `AccessDeniedException` | `20f90ec` |
| ☑ | 2 | Transfer'de negatif tutar kabul ediliyordu | Bean Validation + servis kontrolü | `20f90ec` |
| ☑ | 3 | Herkes her hesaba para yatırabiliyordu | #6 ile kapsandı (yalnızca ADMIN) | `20f90ec` |
| ☑ | 4 | Başkası adına hesap açılabiliyordu | `userId` token'dan alınıyor | `20f90ec` |
| ☑ | 5 | Herkes tüm kullanıcıların bilgisini görebiliyordu | Sahiplik kontrolü (DB sorgusundan önce) | `20f90ec` |
| ☑ | 6 | Herkes kendine sınırsız para yatırabiliyordu | ADMIN rolü | `20f90ec` |
| ☑ | 7 | Tüm hatalar boş 403 dönüyordu | RFC 9457 Problem Details; `GlobalExceptionHandler` + 401/403 yazıcıları | `20f90ec` |
| ☑ | 8 | Eşzamanlı transferlerde güncelleme kaybı | Pessimistic lock + sıralı kilitleme | `20f90ec` |
| ☑ | 9 | Hesap numarası milisaniyeden üretiliyordu, benzersizlik kısıtı yoktu | `ACC-<UUID>` (`@PrePersist`) + `@Table(uniqueConstraints)` | `20f90ec` |
| ☑ | 10 | Aynı hesaba transfer kabul ediliyordu | `@AssertTrue` (400 `VALIDATION_FAILED`) + servis kontrolü | `20f90ec` |
| ☑ | 11 | Kafka consumer hatada mesajı kaybediyordu; retry/DLT yoktu; idempotent değildi | `DefaultErrorHandler` (3 retry, üstel backoff) + DLT + `ErrorHandlingDeserializer` + `existsByUserId` kontrolü | `20f90ec` |
| ☑ | 12 | Kafka gönderimi kullanıcı kaydıyla atomik değildi | Transactional outbox (`OutboxWriter` + `OutboxPublisher`) | `20f90ec` |
| ☑ | 13 | Aynı kullanıcı adıyla kayıt 500 dönüyordu | Ön kontrol → 409 `USERNAME_ALREADY_EXISTS` | `20f90ec` |
| ☑ | 21 | `User.role` kullanılmıyordu | Rol sistemi kuruldu (`USER` / `ADMIN`) | `20f90ec` |
| ☑ | 25 | Rol yetkisi filtreden Spring Security'ye aktarılmıyordu | `authorities` authentication nesnesine verildi | `20f90ec` |
| ☑ | 26 | Filtre her istekte konsola yazıyordu | Satır kaldırıldı | `20f90ec` |
| ☑ | 27 | Hata tipleri tutarsızdı, mesajda yazım hatası | Tipli exception'lar; mesaj düzeltildi | `20f90ec` |
| ☑ | 28 | `AccountController`'da kullanılmayan `JwtService` alanı | Alan kaldırıldı | `20f90ec` |
