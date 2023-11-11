FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY ./microservice-demo/messages-resource/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]