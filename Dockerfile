FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY fantasy-sim-core/pom.xml fantasy-sim-core/pom.xml
COPY fantasy-sim-cli/pom.xml fantasy-sim-cli/pom.xml
COPY fantasy-sim-api/pom.xml fantasy-sim-api/pom.xml
COPY fantasy-sim-core/src fantasy-sim-core/src
COPY fantasy-sim-cli/src fantasy-sim-cli/src
COPY fantasy-sim-api/src fantasy-sim-api/src

RUN mvn -B -pl fantasy-sim-api -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system fantasy \
    && useradd --system --gid fantasy --home-dir /app fantasy \
    && mkdir -p /app/data && chown -R fantasy:fantasy /app
COPY --from=build --chown=fantasy:fantasy \
    /workspace/fantasy-sim-api/target/fantasy-sim-api-2.1.0.jar /app/app.jar

USER fantasy
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8080/api/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
