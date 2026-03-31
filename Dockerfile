# Build frontend first
FROM node:20-alpine AS frontend-builder
WORKDIR /web
COPY frontend-react/package*.json ./
RUN npm install --legacy-peer-deps
COPY frontend-react ./
RUN npm run build

# Build backend with frontend static files
FROM gradle:8.6-jdk17 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon -q || true
COPY src src
# Copy new frontend build into backend static resources
COPY --from=frontend-builder /web/dist /app/src/main/resources/static
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache fontconfig ttf-dejavu
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 7660 37020/udp 8090
ENTRYPOINT ["sh", "-c", "sleep 2 && java -jar app.jar"]
