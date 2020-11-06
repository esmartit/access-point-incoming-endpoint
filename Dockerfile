FROM openjdk:8-jre-slim

EXPOSE 9000

RUN mkdir /app

COPY build/libs/*.jar /app/application.jar
ENV JAVA_OPTS="-Xmx512m -Xms128m -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT exec java $JAVA_OPTS -jar /app/application.jar