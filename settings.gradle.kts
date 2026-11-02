pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "rush-server"

// Spring Boot 모듈만 Gradle 빌드에 포함합니다.
// NestJS(rush-user, rush-sp), Go(rush-live, rush-chat)는 각자 독립 프로젝트입니다.
include("rush-config")
include("rush-eureka")
include("rush-gateway")
include("rush-auth")
include("rush-shop")
include("rush-minigame")
include("rush-stage")
