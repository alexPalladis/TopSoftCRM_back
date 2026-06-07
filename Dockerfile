# ============================================================
# Backend Dockerfile — Multi-stage build
#
# Stage 1 (builder): compiles the Spring Boot app with Maven
# Stage 2 (runtime): runs the JAR on a minimal JRE image
#
# This keeps the final image small (~200MB vs ~600MB)
# and does NOT include Maven, source code, or build tools.
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and pom first (layer cache — only re-downloads
# dependencies when pom.xml changes, not on every code change)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download all dependencies (cached layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build (skipping tests — tests run in CI, not in image build)
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Copy only the final JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Expose the Spring Boot port (nginx proxies to this)
EXPOSE 8080

# Health check — Docker will mark container unhealthy if this fails
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM tuning for a container environment:
#   -XX:+UseContainerSupport   → respect Docker memory limits
#   -XX:MaxRAMPercentage=75.0  → use 75% of the container RAM for heap
#   -Dspring.profiles.active   → activate the prod profile
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]