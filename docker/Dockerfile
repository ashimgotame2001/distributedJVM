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
RUN ./gradlew :node:bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/node/build/libs/microjvm-node-*.jar /app/node.jar
ENV NODE_ID=node-local
ENV GRPC_PORT=9090
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/node.jar"]
