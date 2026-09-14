FROM eclipse-temurin:25-jdk

WORKDIR /build

COPY target/*.jar app.jar

EXPOSE 3030

ENTRYPOINT ["java", "-jar", "app.jar"]