# 빌드 컨텍스트는 저장소 루트입니다: docker build -f rush-chat/Dockerfile .
FROM golang:1.24-alpine AS builder
WORKDIR /workspace/rush-chat

COPY rush-chat/go.mod rush-chat/go.sum* ./
RUN --mount=type=cache,target=/go/pkg/mod go mod download
COPY rush-chat/ ./
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    go mod tidy \
    && CGO_ENABLED=0 GOOS=linux go build -trimpath -o /out/rush-chat ./cmd/server

FROM alpine:3.21 AS runtime
RUN apk --no-cache add ca-certificates tzdata \
    && addgroup -S -g 10001 app && adduser -S -u 10001 app -G app
WORKDIR /app
COPY --chown=app:app --from=builder /out/rush-chat /usr/local/bin/rush-chat
USER app
EXPOSE 8088
ENV PORT=8088
ENTRYPOINT ["rush-chat"]
