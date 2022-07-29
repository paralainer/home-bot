FROM adoptopenjdk/openjdk16
ENV APP_HOME=/root/dev/homebot/
WORKDIR $APP_HOME
COPY gradle ./gradle
COPY gradlew $APP_HOME
COPY build.gradle.kts $APP_HOME
COPY settings.gradle.kts $APP_HOME
COPY gradle.properties $APP_HOME
# download dependencies
RUN ./gradlew build -x test --no-daemon --info || return 0

COPY src ./src
COPY config.yaml ./config.yaml
RUN ./gradlew build -x test --no-daemon --info

EXPOSE 8080
CMD ["java","-jar","/root/dev/homebot/build/libs/home-bot-1.0-SNAPSHOT-standalone.jar"]
