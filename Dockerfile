FROM openjdk:11-jdk AS BUILD_IMAGE
VOLUME /root/.m2
ENV APP_HOME=/root/dev/homebot/
WORKDIR $APP_HOME
COPY .mvn ./.mvn
COPY mvnw $APP_HOME
COPY pom.xml $APP_HOME
# download dependencies
RUN ./mvnw verify -Dmaven.test.skip=true --fail-never
COPY src ./src
RUN ./mvnw package -Dmaven.test.skip=true

FROM openjdk:11-jre-slim
WORKDIR /root/
COPY --from=BUILD_IMAGE /root/dev/homebot/target/home-bot-0.0.1-SNAPSHOT.jar .
EXPOSE 8080
CMD ["java","-jar","home-bot-0.0.1-SNAPSHOT.jar"]
