FROM openjdk:21-jdk-slim
WORKDIR /app

ARG VERSION=prod
ARG BUILD_TIME

COPY build/libs/*-all.jar app.jar

COPY src/main/resources /app/resources

LABEL maintainer="DHC"
LABEL version="${VERSION}"
LABEL build.time="${BUILD_TIME}"
LABEL description="DHC Ktor Application"

ENV APP_VERSION=${VERSION}

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]