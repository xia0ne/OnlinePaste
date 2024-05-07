FROM openjdk:17
MAINTAINER yrh
WORKDIR /
ADD target/OnlinePaste-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar"]
CMD ["app.jar"]