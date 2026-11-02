# rush-eureka

Eureka 서비스 레지스트리. Spring 서비스들이 여기에 등록되고, 게이트웨이는 `lb://서비스이름`으로 찾아갑니다.

대시보드: http://localhost:8761

## 실행

```bash
./gradlew :rush-eureka:bootRun
```
