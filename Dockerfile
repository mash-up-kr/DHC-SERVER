FROM gradle:8.13-jdk21 AS build
WORKDIR /app

ARG VERSION=prod
ARG BUILD_TIME

COPY build.gradle.kts settings.gradle.kts gradle.properties /app/
COPY gradle /app/gradle

RUN gradle build -x test --no-daemon || return 0

COPY src /app/src

RUN gradle buildFatJar -Pversion=${VERSION} --no-daemon

FROM openjdk:21-jdk-slim
WORKDIR /app

ARG VERSION=prod
ARG BUILD_TIME

RUN groupadd -r ktor && useradd -r -g ktor ktor

COPY --from=build /app/build/libs/*-all.jar app.jar

COPY src/main/resources /app/resources

LABEL maintainer="DHC"
LABEL version="${VERSION}"
LABEL build.time="${BUILD_TIME}"
LABEL description="DHC Ktor Application"

RUN chown -R ktor:ktor /app

USER ktor

ENV APP_VERSION=${VERSION}

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]