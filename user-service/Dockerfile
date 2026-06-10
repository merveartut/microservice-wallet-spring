# 1. Aşama: Build (Derleme)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2. Aşama: Run (Çalıştırma)
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
# 'build' aşamasındaki target klasöründen jar'ı kopyala
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]