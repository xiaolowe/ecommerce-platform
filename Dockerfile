# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY platform-common/pom.xml platform-common/pom.xml
COPY platform-security/pom.xml platform-security/pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY services/auth-service/pom.xml services/auth-service/pom.xml
COPY services/catalog-service/pom.xml services/catalog-service/pom.xml
COPY services/inventory-service/pom.xml services/inventory-service/pom.xml
COPY services/order-service/pom.xml services/order-service/pom.xml
COPY services/media-service/pom.xml services/media-service/pom.xml
COPY services/payment-service/pom.xml services/payment-service/pom.xml
COPY services/cart-service/pom.xml services/cart-service/pom.xml

ARG MODULE_PATH
RUN mvn -B -ntp -pl "${MODULE_PATH}" -am dependency:go-offline

COPY . .
RUN mvn -B -ntp -pl "${MODULE_PATH}" -am package -DskipTests
RUN cp "${MODULE_PATH}"/target/*.jar /workspace/application.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /opt/app

RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --create-home app
COPY --from=build /workspace/application.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Djava.security.egd=file:/dev/urandom"
EXPOSE 8080
USER app
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/app/app.jar"]
