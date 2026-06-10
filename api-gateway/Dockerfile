# 1. Aşama: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Tüm projeyi kopyala
COPY . .

# 2. Önce PARENT'ı kur (Modülleri değil, sadece parent pom'u)
RUN mvn install -N -DskipTests

# 3. Sadece api-gateway'i ve ona bağımlı olanları derle
RUN mvn clean package -pl api-gateway -am -DskipTests

# 2. Aşama: Run
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]