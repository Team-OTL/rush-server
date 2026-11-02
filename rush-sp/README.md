# rush-sp

sp 서비스 (NestJS). 포트 8086.

게이트웨이에서 `/sp/**` 요청이 들어오며, 인증된 사용자 ID는 `X-User-Id` 헤더로 전달됩니다.

## 실행

```bash
npm install
npm run start:dev
curl localhost:8086/sp/ping
```
