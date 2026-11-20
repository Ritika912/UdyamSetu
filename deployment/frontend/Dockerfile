# syntax=docker/dockerfile:1
# Build context: the frontend source repository (D:\Projects\UdyamSetu). Its source is never modified.
# Named context "deploy": this deployment folder, used only to copy nginx.conf (see docker-compose.yml).

# ---- Build stage: Node 22 for Vite 8 (requires ^20.19.0 || >=22.12.0) ----
FROM node:22-bookworm-slim@sha256:e9f565d32859f0cbfb52125eafe8be39390f24dac302dc3cdf566e8ba3ab21ee AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# ---- Runtime stage: unprivileged nginx serving the static build and proxying /api ----
FROM nginxinc/nginx-unprivileged:stable-alpine@sha256:f5116ed4606f3eb015dd4a8e28cb9b351476dc0ad4bc6e9f93b19cec130cdf03

COPY --from=build /app/dist /usr/share/nginx/html
COPY --from=deploy nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
