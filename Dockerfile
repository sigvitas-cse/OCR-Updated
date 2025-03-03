# Use Eclipse Temurin JDK 23 as the base image
FROM eclipse-temurin:23-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the target directory to the container
COPY target/root1-0.0.1-SNAPSHOT.jar app.jar

# Expose the application's default port
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
