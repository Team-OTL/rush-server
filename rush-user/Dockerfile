# 빌드 컨텍스트는 저장소 루트입니다: docker build -f rush-user/Dockerfile .
FROM node:22-alpine AS builder
WORKDIR /workspace/rush-user

COPY rush-user/package.json rush-user/package-lock.json ./
RUN --mount=type=cache,id=npm-rush-user,target=/root/.npm npm ci
COPY rush-user/tsconfig.json rush-user/tsconfig.build.json rush-user/nest-cli.json ./
COPY rush-user/src src
RUN npm run build

FROM node:22-alpine AS runtime
RUN addgroup -S -g 10001 app && adduser -S -u 10001 app -G app
WORKDIR /app
COPY rush-user/package.json rush-user/package-lock.json ./
RUN --mount=type=cache,id=npm-rush-user,target=/root/.npm npm ci --omit=dev
COPY --chown=app:app --from=builder /workspace/rush-user/dist ./dist
USER app
EXPOSE 8085
ENV PORT=8085
ENV NODE_ENV=production
ENTRYPOINT ["node", "dist/main.js"]
