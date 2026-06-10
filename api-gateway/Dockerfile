# 1. Aşama: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Tüm projeyi kopyala (Parent POM ve tüm modüller dahil)
COPY . .

# 2. Sadece ilgili servisi derle
# -pl (project list): Sadece api-gateway modülünü derle
# -am (also make): Bu modülün bağımlı olduğu diğer modülleri (örn: common-security) de derle
RUN mvn clean package -pl api-gateway -am -DskipTests

# 2. Aşama: Run
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# 3. Kopyalama yolunu 'api-gateway/' altından başlat
COPY --from=build /app/api-gateway/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]