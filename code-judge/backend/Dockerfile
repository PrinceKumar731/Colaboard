# ── Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Cache dependency layer separately
COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S judge && adduser -S judge -G judge

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /tmp/judge && chown judge:judge /tmp/judge

USER judge

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
