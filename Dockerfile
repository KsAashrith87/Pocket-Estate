# Build the Angular single-page application.
FROM node:24-alpine AS frontend
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Package the Angular assets with the Spring Boot application.
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
COPY --from=frontend /workspace/frontend/dist ./frontend/dist
RUN mvn -B package -DskipTests

# Run as a non-root user in a small JRE image.
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --create-home appuser
COPY --from=build /workspace/target/pocket-estate-0.0.1-SNAPSHOT.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
