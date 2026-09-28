FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon bootJar
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system orderhub && useradd --system --gid orderhub orderhub
COPY --from=build /workspace/build/libs/orderhub-0.1.0.jar app.jar
USER orderhub
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
