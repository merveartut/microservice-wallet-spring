# 1. Aşama: Java 21 tabanlı bir imaj kullan
FROM eclipse-temurin:21-jdk-alpine

# 2. Aşama: Build edilen jar dosyasını hedef dizine kopyala
# (Önce projenizi 'mvn clean package' ile derlediğinizden emin olun)
COPY target/*-SNAPSHOT.jar app.jar

# 3. Aşama: Uygulamayı çalıştır
ENTRYPOINT ["java","-jar","/app.jar"]