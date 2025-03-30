FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:17.0.1-jdk-slim
COPY --from=build target/nutrimom-0.0.1-SNAPSHOT.jar nutrimom.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","nutrimom.jar"]
