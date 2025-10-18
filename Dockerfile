# Etapa de construcción
FROM ubuntu:latest AS build

RUN apt-get update && apt-get install -y openjdk-21-jdk maven

COPY src .
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM openjdk:21-jdk-slim

EXPOSE 8080

COPY --from=build /target/demo-0.0.1-SNAPSHOT.jar.jar demo.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
