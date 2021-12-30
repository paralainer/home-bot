FROM maven:3.8.1-openjdk-11 AS BUILD_IMAGE
VOLUME /root/.m2
ENV APP_HOME=/root/dev/homebot/
WORKDIR $APP_HOME
COPY pom.xml $APP_HOME
# download dependencies
RUN mvn verify -Dmaven.test.skip=true --fail-never
COPY src ./src
RUN mvn package -Dmaven.test.skip=true

FROM openjdk:11-jre-slim
WORKDIR /root/
COPY --from=BUILD_IMAGE /root/dev/homebot/target/home-bot-0.0.1-SNAPSHOT.jar .
EXPOSE 8080
CMD ["java","-jar","home-bot-0.0.1-SNAPSHOT.jar"]
