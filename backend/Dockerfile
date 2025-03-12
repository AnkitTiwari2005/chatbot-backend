# Use an official OpenJDK image as the base
FROM openjdk:17

# Set the working directory inside the container
WORKDIR /app

# Copy all backend files into the container
COPY . .

# Compile the Java code (if needed)
RUN javac ChatBotServer.java

# Run the Java application
CMD ["java", "ChatBotServer"]
