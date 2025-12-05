FROM maven:3.8-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.war ./app.war
COPY --from=build /app/target/dependency/webapp-runner.jar ./webapp-runner.jar
EXPOSE 10000
CMD ["java", "-jar", "webapp-runner.jar", "--port", "10000", "app.war"]
