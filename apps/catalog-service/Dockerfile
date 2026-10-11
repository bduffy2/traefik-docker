# Stage 1: Build the Spring Boot application
FROM gradle:8.8-jdk21-alpine AS builder

WORKDIR /app

# Copy project files
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle
COPY src ./src

# Build executable bootJar
RUN gradle bootJar --no-daemon -x test

# Stage 2: Minimal Java 21 runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
