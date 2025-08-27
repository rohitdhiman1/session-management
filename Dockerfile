# Dockerfile

FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17-alpine AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project files
COPY session-management/pom.xml .
COPY session-management/src ./src

# Build the application
# The -Dmaven.test.skip=true flag is used to skip tests during the build
RUN mvn package -DskipTests

# Stage 2: Create the final, lightweight runtime image
# Use a JRE (Java Runtime Environment) image. It's much smaller than the JDK.
# We also explicitly set the platform here.
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine AS final

# Set the working directory inside the container
WORKDIR /app

# Copy the packaged JAR file from the build stage to the runtime stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Define the command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
