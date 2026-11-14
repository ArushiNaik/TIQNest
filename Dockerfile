# Builds the Spring Boot app (backend/) into a container image.
# Used by the "app" service in docker-compose.yml. Build context = repo root.

# ---- Stage 1: build the jar with Gradle ----
FROM gradle:9.1-jdk21 AS builder
WORKDIR /usr/src/app

# Gradle needs the root settings.gradle (it says include 'backend')
# plus the backend project itself. The frontend is not copied.
COPY settings.gradle .
COPY backend/build.gradle backend/
COPY backend/src backend/src

RUN gradle :backend:bootJar --no-daemon

# ---- Stage 2: small runtime image with only Java and the jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /usr/src/app/backend/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
