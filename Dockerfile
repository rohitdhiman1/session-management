# This file is used to build the Java application into a Docker image.
# It uses a multi-stage build process to keep the final image size small.

FROM maven:3.8.3-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/session-manager-1.0-SNAPSHOT-jar-with-dependencies.jar ./app.jar
EXPOSE 4567
CMD ["java", "-jar", "app.jar"]