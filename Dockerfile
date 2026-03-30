FROM gradle:8.6-jdk17 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon -q || true
COPY src src
RUN gradle bootJar --no-daemon -q

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache fontconfig ttf-dejavu
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 7660 37020/udp 8090
ENTRYPOINT ["sh", "-c", "sleep 2 && java -jar app.jar"]
