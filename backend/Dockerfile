# Use OpenJDK base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy everything from the backend folder
COPY . .
COPY libs/* libs/

# Compile Java files with external JARs
RUN javac -cp "libs/json-20210307.jar:." ChatBotServer.java


# Run the Java application with JAR dependencies
CMD ["java", "-cp", "libs/*:.", "ChatBotServer"]
