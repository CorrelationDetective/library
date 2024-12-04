FROM maven:3.9.9-eclipse-temurin-11-focal AS builder

ENV MAVEN_OPTS="-Dmaven.repo.local=/opt/maven"
ENV M2_HOME="/opt/maven/m2"

WORKDIR /project
COPY . /project

RUN --mount=type=cache,target=/opt/maven/m2 mvn clean package -DskipTests

FROM eclipse-temurin:11-jdk-focal AS runtime
LABEL authors="jens"

# Set working directory
WORKDIR /app

# Copy fat jar
COPY --from=builder /project/target/CorrelationDetective-1.0-jar-with-dependencies.jar /app/cd.jar


# Set the entrypoint to running the jar file
ENTRYPOINT ["java", "-cp", "cd.jar", "core/Main"]

# Pass the arguments to the entrypoint
CMD ["-h"]