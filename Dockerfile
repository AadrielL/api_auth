# syntax=docker/dockerfile:1
# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache de dependências do Maven
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B || true

# Compilação da aplicação
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

# Estágio de Execução
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
