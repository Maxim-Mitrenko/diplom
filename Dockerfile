FROM openjdk:17-oracle

EXPOSE 8090

COPY target/diplom-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]