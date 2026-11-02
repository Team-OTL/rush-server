# rush-live

live 실시간 서비스 (Go + chi + WebSocket). 포트 8087.

## 처음 한 번

```bash
go mod tidy   # go.sum 생성 후 커밋하세요
```

## 구조

```
cmd/server/main.go       # 진입점
internal/router/         # chi 라우터, 미들웨어
internal/health/         # 헬스체크 핸들러
internal/ws/             # WebSocket 허브
internal/config/         # 환경변수
```

## 실행

```bash
make run
curl localhost:8087/live/ping
```

게이트웨이를 거칠 때는 `ws://localhost:8080/live/ws?token=<JWT>` 로 접속합니다.
