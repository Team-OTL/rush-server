# rush-config

Spring Cloud Config Server. `src/main/resources/config/` 아래의 `{서비스이름}.yml` 파일을 각 서비스에 내려줍니다.

## 실행

```bash
./gradlew :rush-config:bootRun
curl localhost:8888/rush-auth/default
```
