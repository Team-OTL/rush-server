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

WebSocket을 제외한 라우트는 서킷브레이커를 거칩니다. 서비스에 연결할 수 없으면 `503`, 5초 안에 응답이 없으면 `504`를 돌려줍니다.

게이트웨이가 만드는 에러는 모두 같은 형식입니다.

```json
{ "code": "UNAUTHORIZED", "message": "인증이 필요합니다." }
```

모든 요청에 `X-Request-Id`를 새로 붙여 서비스와 응답에 넘기고, 메서드·경로·상태·소요 시간·사용자 ID를 한 줄로 남깁니다.

## 실행

```bash
./gradlew :rush-gateway:bootRun
```
