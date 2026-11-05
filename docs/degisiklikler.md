# Değişiklik Kaydı

Bu doküman, 2026-11-02 ve 2026-11-03 tarihlerinde projede yapılan değişiklikleri, nedenlerini ve etkilerini anlatır. Sistemin güncel halinin tam açıklaması için: [Teknik Doküman](teknik-dokuman.md). Açık kalan sorunlar için: [Yapılacaklar](yapilacaklar.md).

## Özet

| Alan | Değişiklik | Durum |
|---|---|---|
| Altyapı | `JWT_SECRET` container'lara aktarıldı | ✅ Commit edildi (`2f5f10d`) |
| Dokümantasyon | README'deki yanlış endpoint düzeltildi | ✅ Commit edildi (`8aa90ac`) |
| Güvenlik | API cevaplarından şifre hash'i kaldırıldı | ✅ Commit edildi (`8c9d2b0`) |
| Güvenlik | Transfer'de kaynak hesap sahipliği kontrolü (#1) | ✅ Test edildi |
| Güvenlik | Transfer'de negatif/sıfır tutar engellendi (#2) | ✅ Test edildi |
| Güvenlik | Para yatırmada yetkisiz erişim kapatıldı (#3) | ✅ #6 ile kapsandı |
| Güvenlik | Hesap açma her zaman token sahibine (#4) | ✅ Test edildi |
| Güvenlik | Kullanıcı bilgisi yalnızca sahibine (#5) | ✅ Test edildi |
| Yetkilendirme | Rol sistemi; para yatırma ADMIN'e kısıtlandı (#6, #21, #25) | ✅ Test edildi |
| Tutarlılık | Bakiye işlemlerinde pessimistic locking (#8) | ✅ Test edildi |
| Doğrulama | Transfer, kayıt ve giriş isteklerine Bean Validation | ✅ Test edildi |
| Hata yönetimi | Global hata altyapısı, RFC 9457 Problem Details (#7, #13, #27) | ✅ Test edildi |
| Doğrulama | Aynı hesaba transfer engellendi (#10) | ✅ Test edildi |
| Mesajlaşma | Kafka consumer: retry, DLT, idempotency (#11) | ✅ Test edildi |
| Temizlik | Debug çıktısı ve kullanılmayan alan kaldırıldı (#26, #28) | ✅ |

**#1 ve sonrasındaki değişiklikler henüz commit edilmedi.**

---

## 1. Altyapı ve Ortam

### 1.1 `JWT_SECRET` container'lara aktarıldı — `2f5f10d`

**Sorun:** `user-service` ve `wallet-service`, `jwt.secret=${JWT_SECRET}` değerini bekliyordu. Ancak `docker-compose.yml` bu değişkeni container'lara geçirmiyordu. Kök dizindeki `.env` dosyasını Compose yalnızca `${...}` interpolasyonu için okur, içeriğini container'a kendiliğinden aktarmaz. Sonuç olarak iki servis açılışta çöküyordu.

**Değişiklik:** `docker-compose.yml`'de iki servisin `environment` bölümüne eklendi:

```yaml
      JWT_SECRET: ${JWT_SECRET}
```

### 1.2 Geliştirme ortamı

Projeyi çalıştırmak ve geliştirmek için kurulan araçlar (macOS, Homebrew):

| Araç | Sürüm | Not |
|---|---|---|
| Docker Desktop | 29.9.2 (Compose v5.6.1) | Sistemi çalıştırmak için zorunlu |
| OpenJDK | 21.0.13 | `/opt/homebrew/opt/openjdk@21`. Maven bağımlılığı olarak JDK 27 de kuruldu; projede **21** kullanılmalı. |
| Maven | 3.10.0 | Yerel build |
| IntelliJ IDEA | 2026.2.4 | SDK ve language level: 21 |
| GitHub CLI | 2.103.3 | Push için kimlik doğrulama |

---

## 2. Dokümantasyon

### 2.1 README endpoint düzeltmesi — `8aa90ac`

README'de var olmayan `GET /api/account/details` yazıyordu. Doğru endpoint `GET /api/account/user/{userId}` olarak düzeltildi ve yalnızca token sahibinin kendi id'siyle 200 döndüğü notu eklendi.

### 2.2 `docs/` dizini

| Dosya | İçerik |
|---|---|
| `docs/teknik-dokuman.md` | Mimari, güvenlik modeli, hata yönetimi, veri modeli, API referansı, kurulum |
| `docs/yapilacaklar.md` | Bilinen sorunların öncelikli takip listesi |
| `docs/degisiklikler.md` | Bu doküman |

---

## 3. Güvenlik Düzeltmeleri

### 3.1 Şifre hash'i API cevaplarından kaldırıldı — `8c9d2b0`

**Sorun:** `POST /api/users/register` ve `GET /api/users/{id}` cevapları BCrypt hash'ini içeriyordu (`"password":"$2a$10$..."`). Register'ın hata dalı ise `body(user)` ile hash'lenmemiş şifreyi de döndürebiliyordu.

**Değişiklik:** `User.java`'da `password` alanı yalnızca okunabilir (JSON'dan alınır, JSON'a yazılmaz) yapıldı:

```java
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
```

Alan entity seviyesinde işaretlendiği için `User` döndüren tüm cevaplar kapsanır. Kafka event'i ayrı bir record (`UserCreatedEvent`) kullandığı için etkilenmedi.

### 3.2 Transfer: kaynak hesap sahipliği (#1)

**Sorun:** Kaynak hesabın token sahibine ait olup olmadığı kontrol edilmiyordu. Hesap numarasını bilen herkes başkasının hesabından kendine para aktarabiliyordu.

**Değişiklik:**

- `AccountController.transfer`, token'daki `userId`'yi servise iletiyor.
- `AccountService.transferMoney(authenticatedUserId, ...)`, kaynak hesabın sahibi farklıysa `AccessDeniedException` fırlatıyor (→ 403 `ACCESS_DENIED`).

Kontrol serviste olduğu için `transferMoney`'yi başka bir yerden çağıran kod da bu kontrolü atlayamaz.

### 3.3 Transfer: tutar doğrulaması (#2) ve Bean Validation

**Sorun:** Negatif tutar kabul ediliyordu. `amount: -100` ile para ters yöne akıyor ve bakiye kontrolü atlanıyordu; testte bir hesap `-66.00` bakiyeye düştü.

**Değişiklik iki katmanlı:**

1. **HTTP katmanı:** `spring-boot-starter-validation` bağımlılığı eklendi (`wallet-service/pom.xml`). `TransferRequest` anotasyonlarla doğrulanıyor ve controller'da `@Valid` kullanılıyor:

    ```java
    public record TransferRequest(
            @NotBlank(message = "Kaynak hesap numarası boş olamaz") String fromAccount,
            @NotBlank(message = "Hedef hesap numarası boş olamaz") String toAccount,
            @NotNull(message = "Transfer tutarı zorunludur")
            @Positive(message = "Transfer tutarı 0'dan büyük olmalıdır") BigDecimal amount
    ) {}
    ```

2. **Servis katmanı:** `transferMoney`'nin başında `amount == null || amount <= 0` kontrolü. HTTP dışından gelen çağrılar için ikinci güvence.

Para yatırmada da aynı kontrol metodun başında yapılıyor; geçersiz istekler hesabı kilitlemeden reddediliyor.

### 3.4 Para yatırma: yetkisiz erişim (#3)

**Sorun:** Herkes herhangi bir hesaba para yatırabiliyordu.

**Değişiklik:** Önce `depositMoney`'ye sahiplik kontrolü eklendi. Ardından #6 kapsamında para yatırma ADMIN'e kısıtlandığında bu kontrol kaldırıldı: admin'in tasarım gereği herhangi bir hesaba para yatırabilmesi gerekiyor. Yetkisiz erişim artık rol kuralıyla engelleniyor.

### 3.5 Hesap açma: hesap her zaman token sahibine (#4)

**Sorun:** `POST /api/account/accounts?userId=...` başka bir kullanıcı adına hesap açmaya izin veriyordu.

**Değişiklik:** `userId` parametresi kaldırıldı; hesabın sahibi token'dan alınıyor (`CurrentUser.id()`).

### 3.6 Kullanıcı bilgisi: yalnızca sahibine (#5)

**Sorun:** `GET /api/users/{id}`, geçerli token'ı olan herkese tüm kullanıcıların kullanıcı adını ve e-postasını gösteriyordu.

**Değişiklik:** `UserController.getUser`, istenen `id` token'daki `userId`'den farklıysa 403 dönüyor. Kontrol veritabanı sorgusundan **önce** yapılıyor. Böylece başkasına ait var olan ve olmayan id'ler aynı cevabı alıyor ve id'ler tahmin edilemiyor.

`/api/users/check/{id}` bilinçli olarak değiştirilmedi: wallet-service hesap açarken bu endpoint'i Feign ile çağırıyor ve sadece `true`/`false` dönüyor.

---

## 4. Rol Sistemi (#6, #21, #25)

**Sorun:** Para yatırma, gerçek bir para kaynağı olmadan bakiyeyi artırıyordu; herkes kendine sınırsız para yatırabiliyordu. `User.role` alanı vardı ama hiç kullanılmıyordu.

**Tasarım:** Para yatırma, dışarıdan gelen parayı sisteme sokan yetkili bir işlem olarak ele alındı ve yalnızca ADMIN'e açıldı.

**Değişiklikler:**

| Modül | Dosya | Değişiklik |
|---|---|---|
| common-security | `JwtService` | `generateToken(userName, userId, role)` — `role` claim'i eklendi; `extractRole(token)` metodu eklendi |
| common-security | `JwtAuthenticationFilter` | Rolden `ROLE_<rol>` yetkisi üretilip authentication nesnesine veriliyor |
| user-service | `UserController.createUser` | Yeni kayıtlara `role = "USER"` |
| user-service | `UserController.login` | Rol token'a ekleniyor |
| wallet-service | `SecurityConfig` | `.requestMatchers(HttpMethod.POST, "/api/account/deposit").hasRole("ADMIN")` |
| wallet-service | `AccountController` / `AccountService` | `depositMoney(accountNumber, amount)`; sahiplik kontrolü kaldırıldı |

Rol bilinçli olarak istemciden alınmıyor (`UserRequest`'te `role` alanı yok). Aksi halde herkes kayıt olurken kendini admin yapabilirdi. ADMIN rolü yalnızca veritabanından atanıyor; adımlar [Teknik Doküman 10.4](teknik-dokuman.md#104-admin-kullanıcı-oluşturma)'te.

### 4.1 Düzeltilen hata #25 — rol yetkisi Spring Security'ye aktarılmıyordu

İlk uygulamada filtre yetki listesini hesaplıyor, ama authentication nesnesine boş listeyi veriyordu. Sonuç olarak `hasRole("ADMIN")` kuralı kimse için sağlanmıyor ve **admin dahil kimse para yatıramıyordu**.

```java
// Önce
new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
// Sonra
new UsernamePasswordAuthenticationToken(userId, null, authorities);
```

**Test sonucu:** ADMIN token'ıyla başka bir kullanıcının hesabına 50.00 yatırıldı → 200, bakiye arttı. USER token'ıyla aynı istek → 403 `ACCESS_DENIED`.

---

## 5. Eşzamanlılık: Pessimistic Locking (#8)

**Sorun:** Bakiye okunup güncellenirken kilit yoktu. Aynı anda gelen iki işlemden birinin güncellemesi kaybolabiliyordu (lost update).

**Değişiklik:**

- `AccountRepository`'ye kilitli okuma metodu eklendi. PostgreSQL'de `SELECT ... FOR UPDATE` olarak çalışır:

    ```java
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findWithLockByAccountNumber(String accountNumber);
    ```

- `depositMoney` ve `transferMoney` artık bu metodu kullanıyor.
- `transferMoney`, iki hesabı **hesap numarasına göre sıralı** kilitliyor. Böylece A→B ve B→A transferleri aynı anda geldiğinde deadlock oluşmuyor.

Optimistic locking (`@Version`) yerine pessimistic locking seçildi. Optimistic yöntemde çakışan işlem hata alır ve tekrar deneme kodu gerekir; para işlemlerinde isteklerin sırayla beklemesi daha basit ve güvenilir.

---

## 6. Global Hata Yönetimi (#7, #13, #27)

**Sorun:** Global bir exception handler yoktu. İş kuralı hataları `RuntimeException` olarak fırlatılıyor, Spring Boot bunları `/error` yoluna yönlendiriyor, `/error` da kimlik doğrulaması istediği için istemci **her hatada boş gövdeli 403** alıyordu. "Bakiye yetersiz", "hesap bulunamadı", "yanlış şifre" ve validation hataları istemci tarafında ayırt edilemiyordu; mesajlar sadece logda görünüyordu. Ayrıca aynı kullanıcı adıyla kayıt 500 dönüyordu (#13) ve hata tipleri tutarsızdı (#27).

**Tasarım:** Spring Boot 3'ün yerleşik desteği olan **RFC 9457 Problem Details** formatı (`application/problem+json`) seçildi. Ortak kod `common-security` modülünde, iki servis de aynı formatı döner. Cevap formatı ve hata kodlarının tam listesi: [Teknik Doküman — Bölüm 9](teknik-dokuman.md#9-hata-yönetimi).

```json
{
  "type": "about:blank",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "Yetersiz bakiye",
  "instance": "/api/account/transfer",
  "errorCode": "INSUFFICIENT_BALANCE",
  "timestamp": "2026-11-03T07:34:19.492940346Z"
}
```

### 6.1 Yeni dosyalar (`common-security`)

| Dosya | Görev |
|---|---|
| `exception/ApiException` | Uygulama hatalarının temel sınıfı (HTTP durumu + `errorCode`) |
| `exception/InvalidRequestException` | 400 `INVALID_REQUEST` |
| `exception/InvalidCredentialsException` | 401 `INVALID_CREDENTIALS` |
| `exception/ResourceNotFoundException` | 404 `RESOURCE_NOT_FOUND` |
| `exception/ConflictException` | 409, kod parametreyle verilir |
| `exception/BusinessRuleException` | 422, kod parametreyle verilir |
| `exception/GlobalExceptionHandler` | `@RestControllerAdvice`, `ResponseEntityExceptionHandler`'dan türer |
| `exception/ProblemDetails` | Ortak cevap üretimi; `errorCode` ve `timestamp` ekler |
| `security/ProblemDetailAuthenticationEntryPoint` | Token yok/geçersiz → 401 |
| `security/ProblemDetailAccessDeniedHandler` | URL kuralında yetki yok → 403 |
| `security/CurrentUser` | `CurrentUser.id()`; controller'lardaki tekrar eden principal kodunun yerini aldı |

`common-security/pom.xml`'e `spring-webmvc`, `spring-tx` ve `jackson-databind` eklendi (sürümler Spring Boot BOM'undan).

### 6.2 Değişen dosyalar

| Dosya | Değişiklik |
|---|---|
| `UserServiceApplication`, `WalletServiceApplication` | `@ComponentScan` `com.wallet.common.security` → `com.wallet.common` (yeni `exception` paketini de kapsasın diye) |
| `AuthConfig`, wallet `SecurityConfig` | `/error` `permitAll`; `exceptionHandling` ile 401/403 yazıcıları bağlandı |
| `UserService` | Kayıtta kullanıcı adı kontrolü → 409 (#13); `authenticate()` metodu → 401; `findById` → 404; `System.err`/`printStackTrace` yerine SLF4J |
| `UserController` | Register'daki 500 dönen `try/catch` kaldırıldı; şifre kontrolü servise taşındı; `@Valid` |
| `UserRepository` | `existsByUsername` |
| `UserRequest`, `AuthRequest` | `@NotBlank`, `@Email` |
| `user-service/pom.xml` | `spring-boot-starter-validation` |
| `AccountService` | `RuntimeException` → `ResourceNotFoundException` / `InvalidRequestException` / `BusinessRuleException("INSUFFICIENT_BALANCE")` (#27); yazım hatası düzeltildi |
| `AccountController` | Boş 403 dönüşleri → `AccessDeniedException`; kullanılmayan `JwtService` alanı kaldırıldı (#28) |

### 6.3 Tasarım kararları

- **İki katman:** Spring Security filtreleri controller'dan önce çalışır; orada oluşan hatalar `@RestControllerAdvice`'a ulaşmaz. Bu yüzden 401/403 için ayrıca `AuthenticationEntryPoint` ve `AccessDeniedHandler` yazıldı.
- **`ResponseEntityExceptionHandler`'dan türetme:** Spring MVC'nin standart hataları (bozuk JSON, eksik parametre, tip uyuşmazlığı, 404, 405, 415) ek kod yazmadan aynı formatta döner.
- **`errorCode` alanı:** `detail` insanlar için, dile ve ifadeye göre değişebilir. İstemciler hata türünü `errorCode` ile ayırt eder.
- **`IllegalArgumentException` yerine `InvalidRequestException`:** Kütüphanelerin fırlattığı `IllegalArgumentException`'lar genellikle sunucu hatasıdır; hepsini 400'e eşlemek gerçek hataları gizlerdi.
- **Giriş hatası tek mesaj:** Kullanıcı yok ve şifre yanlış durumları aynı 401 mesajını döner; kayıtlı kullanıcı adları tahmin edilemez.
- **Yetersiz bakiye 422:** İstek biçim olarak geçerli (400 değil), kaynak var (404 değil), çakışma yok (409 değil); bir iş kuralı ihlal ediliyor.
- **500'de ayrıntı yok:** İstemciye yalnızca "Beklenmeyen bir hata oluştu" döner; stack trace ERROR seviyesinde loglanır.

---

## 7. Transfer: Aynı Hesap Kontrolü (#10)

**Sorun:** Kaynak ve hedef hesabı aynı olan transfer kabul ediliyordu. Bakiye değişmiyordu, ama anlamsız bir işlem başarılı sayılıyordu.

**Değişiklik:**

- `TransferRequest`'e sınıf seviyesinde doğrulama eklendi. Hata 400 `VALIDATION_FAILED` olarak, `errors` listesinde `differentAccounts` alanıyla döner:

    ```java
    @AssertTrue(message = "Kaynak ve hedef hesap aynı olamaz")
    public boolean isDifferentAccounts() {
        return fromAccount == null || !fromAccount.equals(toAccount);
    }
    ```

    `fromAccount == null` durumunda `true` dönülmesinin nedeni: boş alan zaten `@NotBlank` ile raporlanıyor; aynı hata iki kez gösterilmesin.
- `AccountService.transferMoney`'de aynı kontrol tekrarlandı (`InvalidRequestException`, 400 `INVALID_REQUEST`). Servis HTTP dışından çağrılırsa diye ikinci güvence.

---

## 8. Kafka Consumer: Retry, DLT ve Idempotency (#11)

**Sorun:** `UserConsumer`'da üç ayrı sorun vardı:

1. **Hatada mesaj kayboluyordu.** Exception yakalanıp loglanıyor, ack çağrılmıyordu. Koddaki yorum mesajın kuyrukta kalacağını varsayıyordu, ama Kafka'da ack edilmeyen mesaj tekrar gönderilmez; sonraki mesaj ack edildiğinde offset onun da ilerisine geçer. Kullanıcı cüzdansız kalıyordu.
2. **Retry ve DLT yoktu.** Kısa süreli bir veritabanı kesintisi bile mesaj kaybına yol açıyordu.
3. **Idempotent değildi.** Kafka at-least-once teslim eder; aynı event ikinci kez gelirse kullanıcıya ikinci hesap açılıyordu.

Ayrıca bozuk JSON içeren bir mesaj consumer'ı sonsuza kadar kilitleyebilirdi (poison pill).

**Değişiklik:**

| Dosya | Değişiklik |
|---|---|
| `config/KafkaErrorHandlingConfig.java` (yeni) | `DefaultErrorHandler`: 3 tekrar, üstel backoff (1/2/4 sn), sonra `DeadLetterPublishingRecoverer` ile `user-created-topic.DLT`'ye yazma. `InvalidRequestException` tekrar denenmez. DLT topic'i (`NewTopic`) ve DLT'ye yazan `KafkaTemplate` (`DelegatingByTypeSerializer`: JSON veya ham byte) |
| `messaging/UserConsumer.java` | `try/catch` ve `Acknowledgment` kaldırıldı; `userId == null` → `InvalidRequestException`; `existsByUserId` ile idempotency; `@Slf4j` ile loglama |
| `repository/AccountRepository.java` | `existsByUserId(Long userId)` |
| `application.properties` | `ErrorHandlingDeserializer` (delegate: `JsonDeserializer`); `spring.json.value.default.type`; `ack-mode=record` |

Kavramların ve her ayarın ayrıntılı açıklaması: [Kafka Kılavuzu](kafka-kilavuzu.md).

**Tasarım kararları:**

- **Bloklayıcı retry (`DefaultErrorHandler`) seçildi; `@RetryableTopic` seçilmedi.** Toplam bekleme ~7 sn, trafik düşük; ayrı retry topic'lerine gerek yok ve mesaj sırası korunuyor.
- **Idempotency `userId` ile yapıldı; `eventId` + inbox tablosu yapılmadı.** Producer'ı ve iki servisteki event sınıfını değiştirmeyi gerektirirdi. Sınırı: kullanıcı event işlenmeden önce elle hesap açarsa otomatik hesap açılmaz.
- **Eşzamanlılık:** Tek partition ve tek consumer olduğu için `existsByUserId` ile `save` arasında yarış oluşmuyor.

**Kapsam dışı kalanlar:** DLT izleme ve yeniden işleme (#33), producer tarafının atomikliği (#12), `trusted.packages=*` (#16).

---

## 9. API Değişiklikleri (İstemciler İçin)

### 9.1 Endpoint davranışları

| Endpoint | Önce | Sonra | Uyumluluk |
|---|---|---|---|
| `POST /api/users/register` | `"role": null`, `"password": "$2a$..."` | `"role": "USER"`, `password` yok | Geriye uyumlu |
| `POST /api/users/register` | Boş alanlar kabul ediliyordu | `username`, `password` zorunlu; `email` formatı kontrol ediliyor | **Kırıcı** |
| `POST /api/users/login` | Token: `userId`, `sub` | Token: `userId`, `sub`, **`role`** | Geriye uyumlu |
| `GET /api/users/{id}` | Her kullanıcı görülebilir | Yalnızca kendisi | **Kırıcı** |
| `POST /api/account/accounts` | `?userId=..&currency=..` | `?currency=..`; `userId` yok sayılır | Geriye uyumlu (davranış değişti) |
| `POST /api/account/deposit` | Herkes, her hesaba | Yalnızca ADMIN, her hesaba | **Kırıcı** |
| `POST /api/account/transfer` | Her hesaptan, her tutar | Yalnızca kendi hesabından, tutar > 0, alanlar zorunlu, kaynak ≠ hedef | **Kırıcı** |

### 9.2 Hata cevapları — **kırıcı değişiklik**

Tüm hata cevaplarının gövdesi ve çoğunun durum kodu değişti. Önceden hatalarda boş gövde dönüyordu; artık `application/problem+json` dönüyor.

| Durum | Önce | Sonra |
|---|---|---|
| Token yok / geçersiz / süresi dolmuş | 403 | **401** `UNAUTHORIZED` |
| Yanlış şifre veya olmayan kullanıcı ile giriş | 403 | **401** `INVALID_CREDENTIALS` |
| Aynı kullanıcı adıyla kayıt | 500 (gövdede kullanıcı nesnesi) | **409** `USERNAME_ALREADY_EXISTS` |
| Validation hatası | 403 | **400** `VALIDATION_FAILED` + `errors` |
| Geçersiz tutar (para yatırma) | 403 | **400** `INVALID_REQUEST` |
| Kullanıcı / hesap bulunamadı | 403 | **404** `RESOURCE_NOT_FOUND` |
| Yetersiz bakiye | 403 | **422** `INSUFFICIENT_BALANCE` |
| Başkasının kaynağına erişim, rol yetersiz | 403 (boş) | 403 `ACCESS_DENIED` |
| Beklenmeyen hata | 403 | **500** `INTERNAL_ERROR` |

İstemciler 401 aldığında yeniden giriş yaptırmalı, 403'te yetki hatası göstermelidir. Önceden ikisi ayırt edilemiyordu.

---

## 10. Geçiş Adımları

Bu değişiklikleri çalışan bir ortama uygularken:

1. **İki servisi birlikte derleyin** (`common-security` değişti):
    ```bash
    docker compose up -d --build user-service wallet-service
    ```
2. **Mevcut kullanıcılara rol atayın** (rol sisteminden önceki kayıtlarda `role` `null`):
    ```bash
    docker exec user-db psql -U postgres -d user_db -c "UPDATE users SET role = 'USER' WHERE role IS NULL;"
    ```
3. **En az bir admin oluşturun** ([Teknik Doküman 10.4](teknik-dokuman.md#104-admin-kullanıcı-oluşturma)).
4. **Tüm kullanıcılar yeniden giriş yapmalı.** Eski token'larda `role` claim'i yok.
5. **İstemcileri güncelleyin:** Hata ayrımını `errorCode`'a göre yapın; 401 ile 403'ü ayrı ele alın ([9.2](#92-hata-cevapları--kırıcı-değişiklik)).

---

## 11. Test Sonuçları

2026-11-03'te güncel kodla yeniden derlenmiş sistem üzerinde uçtan uca test yapıldı.

### 11.1 Güvenlik ve iş kuralları

| # | Senaryo | Beklenen | Sonuç |
|---|---|---|---|
| — | Yeni kayıt | `role: "USER"`, şifre yok | ✅ |
| — | Token içeriği | `role` claim'i var | ✅ (`USER` / `ADMIN`) |
| 1 | Başkasının hesabından transfer | 403 `ACCESS_DENIED`, bakiye değişmez | ✅ |
| 2 | Negatif tutarla transfer | 400 `VALIDATION_FAILED` | ✅ |
| — | Geçerli transfer | 200 | ✅ |
| 4 | `?userId=<başkası>` ile hesap açma | Hesap token sahibine açılır | ✅ |
| 5 | Başkasının kullanıcı bilgisi | 403 `ACCESS_DENIED` | ✅ |
| 5 | Kendi kullanıcı bilgisi | 200 | ✅ |
| 6 | USER para yatırır | 403 `ACCESS_DENIED` | ✅ |
| 6, 25 | ADMIN başkasının hesabına para yatırır | 200, bakiye artar | ✅ |
| 8 | 11.00 bakiyeden paralel 20 × 1.00 transfer | 11 başarılı, 9 red, toplam korunur | ✅ |
| 8 | Deadlock | Logda yok | ✅ |

### 11.2 Hata cevapları

Tüm hata cevapları `application/problem+json` formatında, `errorCode` ve `timestamp` alanlarıyla döndü.

| Senaryo | Kod | `errorCode` | Sonuç |
|---|---|---|---|
| Aynı kullanıcı adıyla kayıt | 409 | `USERNAME_ALREADY_EXISTS` | ✅ |
| Kayıtta boş alanlar, bozuk e-posta | 400 | `VALIDATION_FAILED` (3 alan hatası) | ✅ |
| Yanlış şifre | 401 | `INVALID_CREDENTIALS` | ✅ |
| Olmayan kullanıcı ile giriş | 401 | `INVALID_CREDENTIALS` (aynı mesaj) | ✅ |
| Bozuk JSON | 400 | `INVALID_REQUEST` | ✅ |
| Token yok / geçersiz token | 401 | `UNAUTHORIZED` | ✅ |
| Başkasının hesap listesi | 403 | `ACCESS_DENIED` | ✅ |
| Olmayan kaynak hesaptan transfer | 404 | `RESOURCE_NOT_FOUND` | ✅ |
| Yetersiz bakiye | 422 | `INSUFFICIENT_BALANCE` | ✅ |
| ADMIN negatif tutar / olmayan hesap | 400 / 404 | `INVALID_REQUEST` / `RESOURCE_NOT_FOUND` | ✅ |
| `amount=abc` / eksik `amount` | 400 | `INVALID_REQUEST` | ✅ |
| Tanımsız yol / yanlış metot / yanlış içerik tipi | 404 / 405 / 415 | `NOT_FOUND` / `METHOD_NOT_ALLOWED` / `UNSUPPORTED_MEDIA_TYPE` | ✅ |
| user-service kapalıyken hesap açma | 500 | `INTERNAL_ERROR`; iç ayrıntı sızmadı, stack trace logda | ✅ |
| Aynı hesaba transfer (#10) | 400 | `VALIDATION_FAILED`, `errors[0].field = differentAccounts` | ✅ |

### 11.3 Kafka consumer (#11)

Mesajlar `kafka-console-producer` ile doğrudan topic'e yazıldı.

| Senaryo | Beklenen | Sonuç |
|---|---|---|
| Açılışta DLT topic'i | `user-created-topic.DLT` oluşur | ✅ |
| Yeni kayıt | Tek hesap açılır | ✅ |
| Aynı `userId` ile event tekrar gönderilir | "zaten var" logu, hesap sayısı 1 kalır | ✅ |
| Bozuk JSON (`bu-json-degil`) | Denenmeden DLT; `DeserializationException` header'ı | ✅ |
| `userId`'siz event | Denenmeden DLT; cause `InvalidRequestException` | ✅ (sonraki event ~7 sn retry beklemeden işlendi) |
| Hatalı mesajlardan sonra yeni kayıt | Consumer takılmaz, hesap açılır | ✅ |
| Geçici hata (veritabanı kapalı) → 4 deneme sonra DLT | | Test edilmedi (birkaç dakika sürüyor) |

---

## 12. Açık Kalanlar

Kritik ve yüksek öncelikte açık madde kalmadı. Orta ve düşük öncelikli maddeler ile bu çalışmada ortaya çıkan yeni maddeler (#29–#31, #33) [Yapılacaklar](yapilacaklar.md)'da.
