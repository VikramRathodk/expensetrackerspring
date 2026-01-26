# Multi-stage build for Kotlin Spring Boot application

# Stage 1: Build stage
FROM eclipse-temurin:24-jdk-alpine AS build

WORKDIR /app

# Install Gradle
RUN apk add --no-cache wget unzip && \
    wget https://services.gradle.org/distributions/gradle-8.12-bin.zip && \
    unzip gradle-8.12-bin.zip && \
    rm gradle-8.12-bin.zip && \
    mv gradle-8.12 /opt/gradle

ENV GRADLE_HOME=/opt/gradle
ENV PATH=$PATH:$GRADLE_HOME/bin

# Copy Gradle files
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application
RUN gradle clean bootJar --no-daemon

# Stage 2: Runtime stage
FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]