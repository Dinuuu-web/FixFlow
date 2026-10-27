# ==============================================================================
# Build Stage
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml and download dependencies for efficient layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy project source code
COPY src ./src

# Build the Spring Boot application (skipping tests for production packaging)
RUN mvn clean package -DskipTests

# ==============================================================================
# Runtime Stage
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

# Security: Create non-root user and group
RUN addgroup -S fixflow && adduser -S fixflow -G fixflow

WORKDIR /app

# Create directory for runtime file uploads and assign ownership
RUN mkdir -p uploads && chown -R fixflow:fixflow /app

# Copy the generated JAR from builder stage
COPY --from=builder --chown=fixflow:fixflow /app/target/fixflow-*.jar app.jar

# Switch to non-root user
USER fixflow

# Default port expected by Render (Render sets $PORT dynamically)
ENV PORT=8080
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# Execute Spring Boot application, dynamically binding to Render's $PORT
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
