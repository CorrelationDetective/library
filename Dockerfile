FROM eclipse-temurin:11-jdk-focal
LABEL authors="jens"

# Set working directory
WORKDIR /app

# Copy jar with dependencies
COPY target/CorrelationDetective-1.0-jar-with-dependencies.jar /app/cd.jar

# Create a directory to store input data
RUN mkdir /app/data

# Set the entrypoint to running the jar file
ENTRYPOINT ["java", "-cp", "/app/cd.jar", "core/Main"]

# Pass the arguments to the entrypoint
CMD ["-h"]

