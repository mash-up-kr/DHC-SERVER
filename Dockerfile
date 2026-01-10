FROM eclipse-temurin:21-jre-alpine
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

ENTRYPOINT ["java", "-jar", "app.jar"]