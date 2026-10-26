# Stage 1: build the WAR with the same Gradle version as the wrapper
FROM gradle:8.8-jdk21 AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle src ./src
RUN gradle war --no-daemon --quiet

# Stage 2: run it on Payara Server, which deploys everything in $DEPLOY_DIR at startup
FROM payara/server-full:7.2026.9
COPY --from=build /home/gradle/project/build/libs/parkpulse.war ${DEPLOY_DIR}/
