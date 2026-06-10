FROM eclipse-temurin:25-jre
LABEL authors="robin"
LABEL service="teggr/articulate:${project.version}"

COPY target/articulate-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]