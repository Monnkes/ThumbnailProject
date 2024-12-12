FROM openjdk:21-jdk-slim

WORKDIR /app

COPY build/libs/oot-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]

EXPOSE 8080
