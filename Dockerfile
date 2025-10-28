
# Stage 1 — build with Gradle
FROM gradle:8.7-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle clean build -x test --no-daemon

# Stage 2 — run the JAR
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S nonroot \
    && adduser -S nonroot -G nonroot
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown -R nonroot:nonroot /app
USER nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
