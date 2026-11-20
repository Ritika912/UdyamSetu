# syntax=docker/dockerfile:1

# ---- Build stage: JDK 21, Maven wrapper, packaged executable jar ----
# Tests are not compiled or run here: they need the test-database account, which must never enter an image build.
FROM eclipse-temurin:21-jdk-noble@sha256:bc12ebd88ffa6dda0092940e179365b05aef8d813b867f9df5a1f168c9a44feb AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q -Dmaven.test.skip=true package \
 && cp target/UdyamSetuBackend-0.0.1-SNAPSHOT.jar /workspace/app.jar

# ---- Runtime stage: JRE 21 only, unprivileged user ----
FROM eclipse-temurin:21-jre-noble@sha256:b4f9d2a0bc273417cf5219b7b832bbf0493ed500b7b57d5001d9f83374659060 AS runtime

RUN groupadd --system udyamsetu \
 && useradd --system --gid udyamsetu --no-create-home --home-dir /app --shell /usr/sbin/nologin udyamsetu

WORKDIR /app
COPY --from=build --chown=root:root /workspace/app.jar /app/app.jar
USER udyamsetu

# No secrets here. Credentials and host-specific values are supplied at run time by Compose (env_file).
# - production profile, so application-prod.yaml and ddl-auto=validate apply.
# - SERVER_ADDRESS=0.0.0.0 overrides the yaml's 127.0.0.1 so the process listens inside the container.
#   The host exposure is controlled by Compose, not by the application.
# - Timezone Asia/Kolkata for the JVM (insight day boundaries use the JVM zone).
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_ADDRESS=0.0.0.0 \
    SERVER_PORT=8080 \
    TZ=Asia/Kolkata \
    JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Kolkata -XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
