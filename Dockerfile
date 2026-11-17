# syntax=docker/dockerfile:1

# ---- Build stage -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# Warm the dependency cache before copying sources so application changes do
# not invalidate the whole download layer. The Maven image ships Maven 3.9,
# so the build does not need the wrapper here.
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Runtime stage ---------------------------------------------------------
# Amazon Corretto publishes linux/amd64 and linux/arm64 Alpine images, unlike
# the Temurin Alpine tag which is amd64-only.
FROM amazoncorretto:17-alpine

RUN addgroup -S app && adduser -S -G app app

WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/OnlinePaste-*.jar app.jar

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
