FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY servico-profissional/pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY servico-profissional/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
