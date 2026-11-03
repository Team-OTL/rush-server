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

WebSocket을 제외한 라우트는 서킷브레이커(Resilience4j)를 거칩니다. 연결 실패와 서비스의 500·502·503·504 응답을 실패로 집계합니다. 최근 20건 중 절반이 실패하면 10초 동안 차단합니다. 응답은 원인별로 나뉩니다. 5초 안에 응답이 없으면 `504`, 서비스가 5xx를 돌려주면 `502`, 차단 중이거나 연결할 수 없으면 `503`입니다.

게이트웨이가 만드는 에러는 모두 같은 형식입니다.

```json
{ "code": "UNAUTHORIZED", "message": "인증이 필요합니다." }
```

모든 요청에 `X-Request-Id`를 새로 붙여 서비스와 응답에 넘기고, 메서드·경로·상태·소요 시간·사용자 ID를 한 줄로 남깁니다.

## 접근 제어

- JWT 검증은 `JwtAuthenticationFilter`가 라우트마다 합니다. Spring Security는 경로 접근 규칙, CORS, 보안 응답 헤더만 맡습니다.
- actuator는 `/actuator/health`만 공개합니다. `gateway`(라우트 조회만), `circuitbreakers`, `info`는 `GATEWAY_ADMIN_ALLOWED_IPS`에 속한 IP에서만 열리고, 그 외에는 `403`을 돌려줍니다.
- 클라이언트 IP는 연결 IP로 판별합니다. 연결 IP가 `TRUSTED_PROXIES`에 속할 때만 `X-Forwarded-For`를 읽으므로, 헤더를 위조해도 IP 제한을 우회할 수 없습니다.
- docker compose로 띄우면 호스트에서 보낸 요청은 도커 브리지 IP로 들어옵니다. 호스트에서 관리용 엔드포인트를 보려면 `GATEWAY_ADMIN_ALLOWED_IPS`에 그 대역을 추가하세요.

## 요청 정화

`RequestSanitizeFilter`가 요청을 서비스로 넘기기 전에 정리합니다.

- 경로 조작(`..`, `%2e`, `%2f`, `%5c`, `//`, `\`)이 있거나 헤더에 제어문자가 있으면 `400`을 돌려줍니다. 8KB를 넘는 헤더는 `431`입니다.
- 클라이언트가 보낸 `X-User-Id`, `X-Real-IP`, `X-Forwarded-*`, `Forwarded`, `X-Internal-*`는 지웁니다. 그 뒤 `X-Real-IP`, `X-Forwarded-For`, `-Proto`, `-Host`, `-Port`를 게이트웨이가 판별한 값으로 다시 씁니다.
- WebSocket용 `?token=`은 검증이 끝나면 지우고 서비스로 넘깁니다.
- 각 서비스의 `/{prefix}/v3/api-docs`는 토큰 없이 접근할 수 있습니다.

## 로그

로그는 JSON 한 줄(logstash 형식)로 stdout에 남깁니다. 요청마다 `X-Request-Id`를 새로 만들어 서비스와 응답에 넘깁니다. 접근 로그는 요청이 끝날 때 한 줄씩 남깁니다.

```json
{"message":"GET /user/ping 200 4ms","requestId":"18e0…","userId":"42","clientIp":"127.0.0.1","method":"GET","path":"/user/ping","status":200,"latencyMs":4,"routeId":"rush-user"}
```

`requestId`, `userId`, `clientIp`는 MDC로 이어지므로, 요청 처리 중 남긴 다른 로그에도 들어갑니다.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `GATEWAY_ADMIN_ALLOWED_IPS` | `127.0.0.1/32,::1/128` | 관리용 actuator 허용 대역 |
| `TRUSTED_PROXIES` | 없음 | 앞단 LB/프록시 대역 |
| `CORS_ALLOWED_ORIGINS` | 없음 | CORS 허용 Origin |

## 실행

```bash
./gradlew :rush-gateway:bootRun
```
