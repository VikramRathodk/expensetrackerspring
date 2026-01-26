# Multi-stage build for Kotlin Spring Boot application

# Stage 1: Build stage
FROM eclipse-temurin:24-jdk-alpine AS build

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

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application with native access flag to suppress warnings
RUN gradle clean bootJar --no-daemon -Dorg.gradle.jvmargs="--enable-native-access=ALL-UNNAMED"

# Stage 2: Runtime stage
FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

# Install PostgreSQL client for wait script
RUN apk add --no-cache postgresql-client bash

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Copy wait script
COPY wait-for-postgres.sh /wait-for-postgres.sh
RUN chmod +x /wait-for-postgres.sh

# Change ownership
RUN chown -R spring:spring /app

USER spring:spring

# Expose port
EXPOSE 8081

# Run the application with wait script
ENTRYPOINT ["/wait-for-postgres.sh", "postgres", "java", "-jar", "app.jar"]