# 빌드 컨텍스트는 저장소 루트입니다: docker build -f rush-sp/Dockerfile .
FROM node:22-alpine AS builder
WORKDIR /workspace/rush-sp

COPY rush-sp/package.json rush-sp/package-lock.json ./
RUN --mount=type=cache,id=npm-rush-sp,target=/root/.npm npm ci
COPY rush-sp/tsconfig.json rush-sp/tsconfig.build.json rush-sp/nest-cli.json ./
COPY rush-sp/src src
RUN npm run build

FROM node:22-alpine AS runtime
RUN addgroup -S -g 10001 app && adduser -S -u 10001 app -G app
WORKDIR /app
COPY rush-sp/package.json rush-sp/package-lock.json ./
RUN --mount=type=cache,id=npm-rush-sp,target=/root/.npm npm ci --omit=dev
COPY --chown=app:app --from=builder /workspace/rush-sp/dist ./dist
USER app
EXPOSE 8086
ENV PORT=8086
ENV NODE_ENV=production
ENTRYPOINT ["node", "dist/main.js"]
