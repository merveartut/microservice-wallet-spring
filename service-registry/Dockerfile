# 1. Aşama: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Tüm projeyi kopyala (Parent POM dahil)
COPY . .

# Root dizininde tüm projeyi derle, böylece parent-child ilişkisi kurulur
# -N (Non-recursive) değil, tam aksine tüm modülleri derliyoruz.
RUN mvn clean install -DskipTests

# 2. Aşama: Run (Burada servis ismini değiştireceksin)
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY --from=build service-registry/target/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]