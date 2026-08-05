FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S hamm && adduser -S hamm -G hamm
COPY --from=build /workspace/target/hamm.jar app.jar
USER hamm
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
