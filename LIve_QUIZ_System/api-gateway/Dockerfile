# Expects `mvn -DskipTests package` to have been run at the repo root first,
# so the fat jar already exists under target/. Keeps the image build fast
# and avoids dragging the whole multi-module source tree into Docker.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

