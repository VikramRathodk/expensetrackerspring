# Build stage
FROM gradle:8.14-jdk21 AS builder

WORKDIR /app

# Copy all gradle files
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Copy source code
COPY src ./src

# Build the application with info logging to see what's happening
RUN gradle clean bootJar --no-daemon --info

# Verify JAR was created and show its location
RUN echo "Contents of build directory:" && \
    find /app/build -name "*.jar" -type f && \
    ls -lah /app/build/libs/

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the JAR from builder - use specific pattern
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Railway provides PORT environment variable
EXPOSE 8080

# Use environment variables for JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Entrypoint with proper JVM configuration for containers
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]