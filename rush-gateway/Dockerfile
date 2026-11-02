# 빌드 컨텍스트는 저장소 루트입니다: docker build -f rush-gateway/Dockerfile .
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build-logic build-logic
COPY settings.gradle.kts build.gradle.kts ./
COPY rush-gateway/build.gradle.kts rush-gateway/build.gradle.kts
COPY rush-gateway/src rush-gateway/src
# 이 이미지에서는 자기 모듈만 빌드하도록 settings를 줄입니다.
RUN sed -i '/^include(/d' settings.gradle.kts \
    && printf 'include("rush-gateway")\n' >> settings.gradle.kts
RUN --mount=type=cache,id=rush-gradle,target=/root/.gradle,sharing=locked \
    chmod +x gradlew \
    && ./gradlew :rush-gateway:bootJar -x test --no-daemon \
    && cp rush-gateway/build/libs/*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN apk --no-cache add curl \
    && addgroup -S -g 10001 app && adduser -S -u 10001 app -G app
WORKDIR /app
COPY --chown=app:app --from=builder /workspace/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
