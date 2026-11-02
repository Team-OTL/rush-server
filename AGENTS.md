# rush-server

Backend services for rush. Each module uses its own stack. See each `rush-*/README.md` and `docker-compose.yml` for module roles, ports, and how to run them.

- Spring Boot + Kotlin: `rush-gateway`, `rush-config`, `rush-eureka`, `rush-auth`, `rush-shop`, `rush-minigame`, `rush-stage`
- NestJS: `rush-user`, `rush-sp`
- Go + chi: `rush-live`, `rush-chat`

## Module Boundaries

- Services never import each other's code directly. Get another service's data through that service's API.
- Keep shared Spring build configuration in the `build-logic/` convention plugins and dependency versions only in `gradle/libs.versions.toml`. Do not write versions in a module's `build.gradle.kts`.
- Each service exposes its API under the same prefix as its gateway route (`/auth`, `/user`, `/chat`, and so on).

## Authentication

- Only `JwtAuthenticationFilter` in `rush-gateway` validates JWTs. Downstream services do not validate JWTs again; they identify the user by the `X-User-Id` header.
- The gateway always strips any client-sent `X-User-Id` and sets it only from a validated token. Do not change this behavior.
- The gateway (8080) is the only externally exposed port. Do not expose service ports.

## Configuration and Secrets

- Spring service configuration lives in `rush-config/src/main/resources/config/{service-name}.yml`; values shared by every Spring service go in `application.yml` in the same directory.
- NestJS and Go services read environment variables directly.
- Never commit secret values. Read them from environment variables and add only the variable name to `.env.example`.

## Verification

These are the same commands CI (`.github/workflows/rush-ci.yml`) runs. Make them pass in every module you changed before committing.

- Kotlin: `./gradlew :rush-xxx:build` (includes ktlint and tests). Fix formatting violations with `./gradlew :rush-xxx:ktlintFormat`. Run Gradle 8.14 on JDK 17–24.
- NestJS: `npm ci`, `npx eslint "{src,test}/**/*.ts"`, `npm run build`, `npm test`, `npm run test:e2e`. Do not use `npm run lint` as a check; it rewrites files with `--fix`.
- Go: `gofmt -l .`, `go vet ./...`, `go test -race ./...`, `go build ./...`. After changing `go.mod`, run `go mod tidy` and commit `go.sum` too.

## Adding a Module

1. Create a `rush-xxx/` directory. CI detects the stack from `build.gradle.kts`, `package.json`, or `go.mod`.
2. For a Spring module, apply `id("rush.kotlin-conventions")`, add `include("rush-xxx")` to the root `settings.gradle.kts`, and add `rush-xxx.yml` to `rush-config`.
3. Add a route to `application.yml` in `rush-gateway` and a service to `docker-compose.yml`.
4. Add `xxx` to the PR title scope list in `rush-ci.yml`.

## Commits

- Format: `type(scope): description`. Use the same scopes as PR titles; the description is Korean, ends in a noun form, and has no trailing period. Example: `feat(gateway): 서비스 장애 시 fallback 응답 추가`
- Keep one logical change per commit. Stage files by name; never use `git add -A` or `git add .`.
- Never add AI attribution: no `Co-Authored-By` trailer for an AI and no "Generated with" line in commits or PR bodies.
- See `.agents/shared/commit-conventions.md` for choosing the type and scope. Where it differs, this file wins.

## Branches

- Never commit directly to `main`. Branch off the remote `main`, work there, and merge through a PR.
- Name branches `<type>/<kebab-case-description>`, using the same type as the commit. Examples: `feat/gateway-fallback`, `fix/jwt-public-path`, `docs/agents-md`
- Keep one piece of work per branch. Do not reuse a branch that has already been merged.

## Pull Requests

- Titles use the `[scope] title` format. Pick scopes from `global`, `gateway`, `config`, `eureka`, `auth`, `shop`, `minigame`, `stage`, `user`, `sp`, `live`, `chat`, `ci/cd`, separating multiple scopes with commas. Do not use a colon (`:`) in the title.
- Bodies follow the overview (개요) and body (본문) sections of `.github/PULL_REQUEST_TEMPLATE.md`.
