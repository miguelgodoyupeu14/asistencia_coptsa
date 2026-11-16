FROM node:22.20-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/asistencia-web/browser /usr/share/nginx/html
EXPOSE 80
