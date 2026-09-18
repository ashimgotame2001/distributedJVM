# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY proto ./proto
COPY common ./common
COPY sdk ./sdk
COPY runtime ./runtime
COPY node ./node
COPY fabric ./fabric
COPY samples ./samples
COPY samples-tasks ./samples-tasks
RUN ./gradlew :node:bootJar :samples-tasks:jar --no-daemon \
 && mkdir -p /out/artifacts/com/microjvm/samples-tasks/0.1.0 \
 && cp samples-tasks/build/libs/samples-tasks-*.jar \
      /out/artifacts/com/microjvm/samples-tasks/0.1.0/samples-tasks.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/node/build/libs/microjvm-node-*.jar /app/node.jar
COPY --from=build /out/artifacts /app/artifacts
ENV NODE_ID=node-local
ENV GRPC_PORT=9090
ENV ARTIFACT_ROOT=/app/artifacts
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/node.jar"]
