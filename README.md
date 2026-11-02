# rush-server

rush 백엔드 모노레포입니다. 서비스마다 기술 스택이 다르고, Spring Boot 모듈만 루트 Gradle 멀티모듈로 묶여 있습니다.

## 서비스

| 모듈 | 스택 | 포트 | 역할 |
|---|---|---|---|
| `rush-gateway` | Spring Cloud Gateway (Kotlin) | 8080 | 외부 진입점, JWT 검증, 라우팅 |
| `rush-config` | Spring Cloud Config (Kotlin) | 8888 | 설정 서버 |
| `rush-eureka` | Eureka Server (Kotlin) | 8761 | 서비스 레지스트리 |
| `rush-auth` | Spring Boot (Kotlin) | 8081 | 인증 |
| `rush-shop` | Spring Boot (Kotlin) | 8082 | 상점 |
| `rush-minigame` | Spring Boot (Kotlin) | 8083 | 미니게임 |
| `rush-stage` | Spring Boot (Kotlin) | 8084 | 스테이지 |
| `rush-user` | NestJS | 8085 | 유저 |
| `rush-sp` | NestJS | 8086 | sp |
| `rush-live` | Go + chi (WebSocket) | 8087 | 라이브 |
| `rush-chat` | Go + chi (WebSocket) | 8088 | 채팅 |

## 구조

```
rush-server/
├── settings.gradle.kts      # Spring 모듈만 include
├── build.gradle.kts
├── gradle/libs.versions.toml # 버전 카탈로그 (Spring 모듈 공통 버전)
├── build-logic/             # 공통 Gradle 설정 (convention plugin)
│   └── src/main/kotlin/
│       ├── rush.spring-boot-conventions.gradle.kts
│       ├── rush.kotlin-conventions.gradle.kts
│       └── rush.kotlin-jpa-conventions.gradle.kts
├── rush-gateway/  rush-config/  rush-eureka/       # Spring 인프라
├── rush-auth/  rush-shop/  rush-minigame/  rush-stage/  # Spring 서비스
├── rush-user/  rush-sp/      # NestJS (각자 package.json)
├── rush-live/  rush-chat/    # Go (각자 go.mod)
└── docker-compose.yml
```

## 요청 흐름

```
Client → rush-gateway(8080) ── JWT 검증 → X-User-Id 헤더 추가
           ├─ /auth, /shop, /minigame, /stage → Eureka(lb://) → Spring 서비스
           ├─ /user, /sp                      → NestJS 서비스
           └─ /live, /chat (WebSocket)        → Go 서비스
```

- Spring 서비스는 시작할 때 `rush-config`에서 설정을 받고 `rush-eureka`에 등록됩니다.
- NestJS/Go 서비스는 게이트웨이가 주소로 직접 연결합니다.
- 뒤쪽 서비스는 JWT를 다시 검증하지 않고 `X-User-Id` 헤더만 읽습니다.

## 실행

### 전체 (Docker)

```bash
cp .env.example .env
docker compose up --build
curl localhost:8080/auth/ping
```

### 개발할 때 (하나씩)

```bash
# 1. 인프라 먼저
./gradlew :rush-config:bootRun
./gradlew :rush-eureka:bootRun

# 2. 작업할 서비스
./gradlew :rush-auth:bootRun
cd rush-user && npm install && npm run start:dev
cd rush-chat && go mod tidy && make run

# 3. 게이트웨이
./gradlew :rush-gateway:bootRun
```

## 새 Spring 모듈 추가하기

1. `rush-xxx/` 폴더를 만들고 `build.gradle.kts`에 `id("rush.kotlin-conventions")` 적용
2. 루트 `settings.gradle.kts`에 `include("rush-xxx")` 추가
3. `rush-config/src/main/resources/config/rush-xxx.yml` 추가
4. `rush-gateway`의 `application.yml`에 라우트 추가
5. `docker-compose.yml`에 서비스 추가
