# Sandbox image for Java submissions.
# Runs as an unprivileged user; no shell utilities beyond what the JDK ships.
FROM eclipse-temurin:17-jdk-alpine

RUN addgroup -S sandbox && adduser -S sandbox -G sandbox \
    && apk del wget curl 2>/dev/null || true

WORKDIR /code

USER sandbox
