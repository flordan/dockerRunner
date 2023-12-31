FROM gradle:8.5.0-jdk17-jammy as builder

COPY . /home/gradle/project
WORKDIR /home/gradle/project
RUN gradle clean build \
    && mkdir -p /home/gradle/project/app/build/distributions/tar \
    && tar xvf /home/gradle/project/app/build/distributions/app-1.0.tar -C /home/gradle/project/app/build/distributions/tar


FROM openjdk:19-jdk-oracle
COPY --from=builder /home/gradle/project/app/build/distributions/tar/app-1.0/lib /app/libs
CMD ["java", "-classpath", "/app/libs/app-1.0.jar:\
/app/libs/commons-io-2.6.jar:\
/app/libs/commons-lang3-3.12.0.jar:\
/app/libs/commons-logging-1.2.jar:\
/app/libs/docker-java-3.3.2.jar:\
/app/libs/docker-java-api-3.3.2.jar:\
/app/libs/docker-java-core-3.3.2.jar:\
/app/libs/docker-java-transport-3.3.2.jar:\
/app/libs/docker-java-transport-jersey-3.3.2.jar:\
/app/libs/docker-java-transport-httpclient5-3.3.2.jar:\
/app/libs/guava-32.1.2-jre.jar:\
/app/libs/hk2-api-2.6.1.jar:\
/app/libs/hk2-locator-2.6.1.jar:\
/app/libs/hk2-utils-2.6.1.jar:\
/app/libs/httpcore5-5.0.2.jar:\
/app/libs/httpclient5-5.0.3.jar:\
/app/libs/jackson-annotations-2.10.3.jar:\
/app/libs/jackson-core-2.10.3.jar:\
/app/libs/jackson-databind-2.10.3.jar:\
/app/libs/jackson-jaxrs-base-2.10.3.jar:\
/app/libs/jackson-jaxrs-json-provider-2.10.3.jar:\
/app/libs/jakarta.activation-api-1.2.1.jar:\
/app/libs/jakarta.annotation-api-1.3.5.jar:\
/app/libs/jakarta.inject-2.6.1.jar:\
/app/libs/jakarta.ws.rs-api-2.1.6.jar:\
/app/libs/jakarta.xml.bind-api-2.3.2.jar:\
/app/libs/jersey-apache-connector-2.30.1.jar:\
/app/libs/jersey-client-2.30.1.jar:\
/app/libs/jersey-common-2.30.1.jar:\
/app/libs/jersey-hk2-2.30.1.jar:\
/app/libs/junixsocket-common-2.6.1.jar:\
/app/libs/junixsocket-native-common-2.6.1.jar:\
/app/libs/role-runner-1.0.jar:\
/app/libs/role-runner-docker-1.0.jar:\
/app/libs/slf4j-api-1.7.30.jar", "nesteddocker.App"]



