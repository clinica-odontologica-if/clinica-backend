FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app


COPY pom.xml .
RUN mvn -DskipTests dependency:go-offline

# Copia a pasta de código-fonte interna
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Busca o arquivo jar gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app/app.jar"]