# rush-gateway

Spring Cloud Gateway. 외부 요청을 받는 유일한 입구이며, JWT 검증 후 `X-User-Id` 헤더를 붙여 각 서비스로 라우팅합니다.

| 경로 | 대상 |
|---|---|
| `/auth/**` | rush-auth (Eureka) |
| `/shop/**` | rush-shop (Eureka) |
| `/minigame/**` | rush-minigame (Eureka) |
| `/stage/**` | rush-stage (Eureka) |
| `/user/**` | rush-user (NestJS) |
| `/sp/**` | rush-sp (NestJS) |
| `/live/**` | rush-live (Go, WebSocket) |
| `/chat/**` | rush-chat (Go, WebSocket) |

## 실행

```bash
./gradlew :rush-gateway:bootRun
```
