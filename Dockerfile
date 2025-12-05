FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/target/*.war ./app.war
COPY --from=build /app/target/dependency/webapp-runner.jar ./webapp-runner.jar
ENV PORT=10000
EXPOSE 10000
CMD java -jar webapp-runner.jar --port ${PORT} app.war
