FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/*.jar app.jar
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "/app.jar"]
