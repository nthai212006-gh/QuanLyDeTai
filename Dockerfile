# ==============================================================================
# STAGE 1: Build the Spring Boot application using Maven and Temurin JDK 17
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache Maven dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build executable jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# STAGE 2: Lightweight Production Runtime Image
# ==============================================================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Set timezone to Vietnam/Asia/Ho_Chi_Minh
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create a non-root application user for enhanced security
RUN groupadd -r spring && useradd -r -g spring spring

# Create directory for uploads and set permissions
RUN mkdir -p /app/uploads && chown -R spring:spring /app

# Copy the built jar from Stage 1
COPY --from=builder /app/target/QuanLyDeTai-*.jar /app/app.jar
RUN chown spring:spring /app/app.jar

USER spring:spring

EXPOSE 8089

# Optimized JVM memory arguments for small VPS (1GB - 2GB RAM)
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
