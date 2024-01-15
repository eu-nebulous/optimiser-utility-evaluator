#
# Build stage
#
FROM docker.io/library/maven:3.9.2-eclipse-temurin-17 AS build
COPY src /home/app/src
COPY local-settings.xml /home/local-settings.xml
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml -s /home/local-settings.xml clean package

#
# Package stage
#
FROM docker.io/library/eclipse-temurin:17-jre
COPY --from=build /home/app/target/utilityevaluator-0.0.1-SNAPSHOT.jar /usr/local/lib/utilityevaluator.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/local/lib/utilityevaluator.jar"]
