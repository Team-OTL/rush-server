# rush-server

모듈마다 스택이 다른 백엔드 모노레포입니다. 모듈별 역할, 포트, 실행 방법은 각 `rush-*/README.md`와 `docker-compose.yml`을 확인하세요.

- Spring Boot + Kotlin: `rush-gateway`, `rush-config`, `rush-eureka`, `rush-auth`, `rush-shop`, `rush-minigame`, `rush-stage`
- NestJS: `rush-user`, `rush-sp`
- Go + chi: `rush-live`, `rush-chat`

## 모듈 경계

- 서비스끼리 서로의 코드를 직접 import하지 않습니다. 다른 서비스의 데이터는 그 서비스의 API로 받습니다.
- Spring 모듈의 공통 빌드 설정은 `build-logic/` convention 플러그인에, 의존성 버전은 `gradle/libs.versions.toml`에만 둡니다. 모듈 `build.gradle.kts`에 버전을 직접 쓰지 않습니다.
- 각 서비스는 게이트웨이 경로와 같은 prefix(`/auth`, `/user`, `/chat` 등)로 API를 노출합니다.

## 인증

- JWT는 `rush-gateway`의 `JwtAuthenticationFilter`에서만 검증합니다. 뒤쪽 서비스는 JWT를 다시 검증하지 않고 `X-User-Id` 헤더로 사용자를 식별합니다.
- 게이트웨이는 클라이언트가 보낸 `X-User-Id`를 항상 제거한 뒤, 검증된 값만 다시 넣습니다. 이 동작을 바꾸지 않습니다.
- 외부에 여는 포트는 게이트웨이(8080)뿐입니다. 서비스 포트를 외부에 노출하지 않습니다.

## 설정과 시크릿

- Spring 서비스 설정은 `rush-config/src/main/resources/config/{서비스이름}.yml`에, 모든 Spring 서비스가 공유하는 값은 같은 폴더의 `application.yml`에 둡니다.
- NestJS, Go 서비스는 환경 변수를 직접 읽습니다.
- 시크릿 값은 커밋하지 않습니다. 환경 변수로 받고, 이름만 `.env.example`에 추가합니다.

## 검증

CI(`.github/workflows/rush-ci.yml`)와 같은 명령입니다. 변경한 모듈에서 통과시킨 뒤 커밋합니다.

- Kotlin: `./gradlew :rush-xxx:build` (ktlint와 테스트 포함). 포맷 위반은 `./gradlew :rush-xxx:ktlintFormat`으로 고칩니다. Gradle 8.14는 JDK 17~24에서 실행합니다.
- NestJS: `npm ci`, `npx eslint "{src,test}/**/*.ts"`, `npm run build`, `npm test`, `npm run test:e2e`. `npm run lint`는 `--fix`로 파일을 수정하므로 검사용으로 쓰지 않습니다.
- Go: `gofmt -l .`, `go vet ./...`, `go test -race ./...`, `go build ./...`. `go.mod`를 바꾸면 `go mod tidy` 후 `go.sum`도 커밋합니다.

## 새 모듈 추가

1. `rush-xxx/` 폴더를 만듭니다. CI는 `build.gradle.kts`, `package.json`, `go.mod`로 스택을 자동 판별합니다.
2. Spring 모듈이면 `id("rush.kotlin-conventions")`를 적용하고, 루트 `settings.gradle.kts`에 `include("rush-xxx")`, `rush-config`에 `rush-xxx.yml`을 추가합니다.
3. `rush-gateway`의 `application.yml`에 라우트를, `docker-compose.yml`에 서비스를 추가합니다.
4. PR 제목 검사의 scope 목록(`rush-ci.yml`)에 `xxx`를 추가합니다.

## 브랜치

- `main`에 직접 커밋하지 않습니다. 원격 `main`에서 새 브랜치를 만들어 작업하고 PR로 머지합니다.
- 이름은 `<type>/<kebab-case-설명>` 형식이고, type은 커밋 type과 같습니다. 예: `feat/gateway-fallback`, `fix/jwt-public-path`, `docs/agents-md`
- 브랜치 하나에는 작업 하나만 담습니다. 이미 머지된 브랜치는 다시 쓰지 않습니다.

## PR

- 제목은 `[scope] 제목` 형식입니다. scope는 `global`, `gateway`, `config`, `eureka`, `auth`, `shop`, `minigame`, `stage`, `user`, `sp`, `live`, `chat`, `ci/cd` 중에서 고르고, 여러 개면 쉼표로 구분합니다. 제목에 콜론(`:`)을 쓰지 않습니다.
- 본문은 `.github/PULL_REQUEST_TEMPLATE.md`의 개요, 본문 형식을 따릅니다.
