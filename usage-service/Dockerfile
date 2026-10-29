# syntax=docker/dockerfile:1.7
# ============================================================
#  usage-service — multi-stage image (build + runtime)
# ============================================================
# Stage 1 — build the executable jar.
# NOTE: the Maven wrapper is deliberately NOT used: it needs
# .mvn/wrapper/maven-wrapper.properties, which is not committed for every
# service. The official Maven image ships the same Maven line.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
# Tests need infrastructure (H2 / Testcontainers), so they are skipped inside the
# image build by default: run them on the host with `.\mvnw.cmd test`.
# Override with `docker compose build --build-arg SKIP_TESTS=false`.
ARG SKIP_TESTS=true
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests=${SKIP_TESTS} package

# Stage 2 — runtime: JRE 21 + curl (used by the HEALTHCHECK) and a non-root user.
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --uid 1001 --create-home --shell /usr/sbin/nologin spring
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER spring
EXPOSE 8083
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"
HEALTHCHECK --interval=10s --timeout=3s --start-period=45s --retries=6 \
    CMD curl -fsS http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
