FROM openjdk:17
MAINTAINER yrh
WORKDIR /
ADD springboot/OnlinePaste.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar"]
CMD ["app.jar"]