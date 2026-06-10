# 💳 Microservice Wallet Application
Java 21, Spring Boot 3.2.4 ve Spring Cloud kullanılarak geliştirilmiş dijital cüzdan sistemi.

## 🏗️ Mimari Yapı
- **API Gateway:** İstek yönlendirme ve merkezi giriş.
- **Service Registry:** Eureka ile servis kaydı.
- **User Service:** Kullanıcı yönetimi.
- **Wallet Service:** Hesap ve transfer yönetimi.
- **Common Security:** Her iki serviste kullanılmak üzere ortak security filter ve Jwt service içeren bir Maven modulü.
- **Event Bus:** Kafka ile servisler arası asenkron iletişim

## 🚀 Nasıl Çalıştırabilirsin
1. Docker Desktop'ı başlatın.
2. `docker-compose up -d --build`
3. **Servis Durumu:** Eureka Dashboard üzerinden (http://localhost:8761) tüm servislerin UP olduğunu teyit edin.

<img width="1641" height="236" alt="image" src="https://github.com/user-attachments/assets/c9faf208-e549-49d3-a407-3661d9ac8f15" />


## Nasıl Test Edebilirsin?
Tüm istekler `API Gateway` (localhost:8080) üzerinden yapılmalıdır.

**1. Kullanıcı Yönetimi**
- Kayıt Ol: `POST http://localhost:8080/api/users/register`<br>
  _Body:_ {"username": "merve", "email": "merve@example.com", "password": "123"}

- Giriş Yap (JWT Al):
  `POST http://localhost:8080/api/users/login`<br>
  _Alınan token ile sonraki işlemlerde Authorization: Bearer <TOKEN> header'ı kullanılmalıdır._

**2. Cüzdan ve Hesap İşlemleri**
- Hesap Durumu:
`GET http://localhost:8080/api/account/details`<br>
  _(JWT token gereklidir)_

**3. Event-Driven Kontrolü**
Bir kullanıcı kayıt olduğunda:

1. User Service bir event fırlatır.

2. Kafka mesajı yakalar.

3. Wallet Service mesajı tüketir ve kullanıcıya otomatik bir cüzdan hesabı oluşturur.
