FROM openjdk:17.0.1-jdk-slim

WORKDIR /app

COPY target/config-server-0.0.1-SNAPSHOT.jar app.jar

RUN apt-get update && apt-get install -y curl

ENTRYPOINT [ "java","-jar","app.jar" ]