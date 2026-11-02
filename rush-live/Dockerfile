# 빌드 컨텍스트는 저장소 루트입니다: docker build -f rush-live/Dockerfile .
FROM golang:1.24-alpine AS builder
WORKDIR /workspace/rush-live

COPY rush-live/go.mod rush-live/go.sum* ./
RUN --mount=type=cache,target=/go/pkg/mod go mod download
COPY rush-live/ ./
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    go mod tidy \
    && CGO_ENABLED=0 GOOS=linux go build -trimpath -o /out/rush-live ./cmd/server

FROM alpine:3.21 AS runtime
RUN apk --no-cache add ca-certificates tzdata \
    && addgroup -S -g 10001 app && adduser -S -u 10001 app -G app
WORKDIR /app
COPY --chown=app:app --from=builder /out/rush-live /usr/local/bin/rush-live
USER app
EXPOSE 8087
ENV PORT=8087
ENTRYPOINT ["rush-live"]
