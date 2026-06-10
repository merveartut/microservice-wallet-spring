# 1. Aşama: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Önce sadece ana pom.xml'i ve parent konfigürasyonunu kopyala
COPY pom.xml .

# 2. Sonra diğer her şeyi kopyala
COPY . .

# 3. ÖNCE PARENT PROJEYİ KUR (Install)
# Bu komut 'microservice-wallet-parent'ı yerel Maven reposuna yükler.
RUN mvn install -N -DskipTests

# 4. ŞİMDİ SERVİSİ DERLE
# Sadece ilgili servisi derle
RUN mvn clean package -pl wallet-service -am -DskipTests

# 2. Aşama: Run
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
# Burada servis ismini kendi servisine göre ayarla (örn: service-registry/)
COPY --from=build /app/wallet-service/target/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]