# Multi-stage build for Kotlin Spring Boot application

# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Install Gradle 8.14
RUN apk add --no-cache wget unzip && \
    wget https://services.gradle.org/distributions/gradle-8.14-bin.zip && \
    unzip gradle-8.14-bin.zip && \
    rm gradle-8.14-bin.zip && \
    mv gradle-8.14 /opt/gradle

ENV GRADLE_HOME=/opt/gradle
ENV PATH=$PATH:$GRADLE_HOME/bin

# Copy Gradle files
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application
RUN gradle clean bootJar --no-daemon

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install bash and postgresql-client for wait script
RUN apk add --no-cache bash postgresql-client

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Copy wait script
COPY wait-for-postgres.sh /wait-for-postgres.sh
RUN chmod +x /wait-for-postgres.sh && chown spring:spring /wait-for-postgres.sh

# Change ownership
RUN chown -R spring:spring /app

USER spring:spring

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application with wait script
ENTRYPOINT ["/wait-for-postgres.sh", "postgres", "java", "-jar", "app.jar"]