# syntax=docker/dockerfile:1
# 前端镜像：Vite 构建 → Nginx 托管静态产物并反向代理 /api（构建上下文为仓库根目录）

# ---------- 1. 构建 ----------
FROM node:22-alpine AS build
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# vite.config.js 的 outDir 指向后端静态目录（供单镜像方案使用），本镜像改为独立 dist
RUN npm run build -- --outDir dist

# ---------- 2. 运行 ----------
FROM nginx:1.27-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/frontend/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD ["sh", "-c", "wget -q -O /dev/null http://127.0.0.1/ || exit 1"]
