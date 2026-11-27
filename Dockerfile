FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /build
COPY ${SERVICE}/pom.xml .
RUN mvn -q dependency:go-offline
COPY ${SERVICE}/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar /app/service.jar
USER 10001
ENTRYPOINT ["java","-jar","/app/service.jar"]
