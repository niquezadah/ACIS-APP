FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /build
COPY ${SERVICE}/pom.xml .
RUN mvn -q dependency:go-offline
COPY ${SERVICE}/src ./src
RUN mvn -q -DskipTests package
COPY deployment/DatabaseTlsProbe.java /build/DatabaseTlsProbe.java
RUN javac -d /build/probe /build/DatabaseTlsProbe.java \
    && mvn -q dependency:copy-dependencies -DincludeArtifactIds=postgresql -DoutputDirectory=/build/probe

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar /app/service.jar
COPY --from=build /build/probe /app/probe
COPY deployment/start.sh /app/start.sh
RUN sed -i 's/\r$//' /app/start.sh
USER 10001
ENTRYPOINT ["/bin/sh","/app/start.sh"]
