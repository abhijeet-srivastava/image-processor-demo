# Start with a base image containing Java runtime (JDK 11 is used here)
FROM openjdk:17-jdk-slim

# Add Maintainer Info
LABEL maintainer="abhijeet.srivastva05@gmail.com"

# The application's jar file
ARG JAR_FILE=target/*.jar

# Add the application's jar to the container
COPY ${JAR_FILE} app.jar

# Information of the running port of the service
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java","-jar","/app.jar"]