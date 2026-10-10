FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -ntp -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/riddler-server/target/riddler-server-3.0.0-SNAPSHOT.jar /app/app.jar
EXPOSE 7070
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
