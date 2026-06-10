# 💳 Microservice Wallet Application
Java 21, Spring Boot 3.2.4 ve Spring Cloud kullanılarak geliştirilmiş dijital cüzdan sistemi.

## 🏗️ Mimari Yapı
- **API Gateway:** İstek yönlendirme ve merkezi giriş.
- **Service Registry:** Eureka ile servis kaydı.
- **User Service:** Kullanıcı yönetimi.
- **Wallet Service:** Hesap ve transfer yönetimi.
- **Common Security:** Her iki serviste kullanılmak üzere ortak security filter ve Jwt service içeren bir Maven modulü.

## 🚀 Nasıl Çalıştırabilirsin
1. Docker Desktop'ı başlatın.
2. `docker-compose up` 
3. Servisleri sırasıyla çalıştırın: Registry -> User -> Wallet -> Gateway.
