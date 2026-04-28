# syntax=docker/dockerfile:1.7

FROM node:25-bookworm-slim AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM deps AS build
COPY index.html vite.config.js ./
COPY src ./src
RUN npm run build

FROM eclipse-temurin:25-jdk AS upr-fvx
ARG TARGETARCH=amd64
ARG UPR_FVX_VERSION=1.5.1
ARG UPR_FVX_TAG=vFVX1.5.1
SHELL ["/bin/sh", "-eux", "-c"]
RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates curl unzip \
  && rm -rf /var/lib/apt/lists/*
RUN case "${TARGETARCH}" in \
    amd64) UPR_FVX_OS="Linux_x86" ;; \
    arm64) UPR_FVX_OS="Linux_ARM" ;; \
    *) echo "Unsupported Docker target architecture: ${TARGETARCH}" >&2; exit 1 ;; \
  esac; \
  UPR_FVX_FILE="UPR_FVX-v$(printf '%s' "${UPR_FVX_VERSION}" | tr . _)-${UPR_FVX_OS}.zip"; \
  UPR_FVX_URL="https://github.com/upr-fvx/universal-pokemon-randomizer-fvx/releases/download/${UPR_FVX_TAG}/${UPR_FVX_FILE}"; \
  mkdir -p /upr-fvx /tmp/upr-fvx-release; \
  curl -fsSL "${UPR_FVX_URL}" -o /tmp/upr-fvx.zip; \
  unzip -q /tmp/upr-fvx.zip -d /tmp/upr-fvx-release; \
  UPR_FVX_ROOT="$(dirname "$(find /tmp/upr-fvx-release -type f -name UPR-FVX.jar | head -n 1)")"; \
  test -n "${UPR_FVX_ROOT}"; \
  cp -R "${UPR_FVX_ROOT}/." /upr-fvx/; \
  test -f /upr-fvx/UPR-FVX.jar; \
  chmod +x /upr-fvx/java/bin/java; \
  test -x /upr-fvx/java/bin/java; \
  rm -rf /tmp/upr-fvx.zip /tmp/upr-fvx-release
COPY patches/fvx /patches/fvx
COPY scripts/build-fvx.sh /scripts/build-fvx.sh
RUN UPR_FVX_PATCH_DIR=/patches/fvx UPR_FVX_OUTPUT=/upr-fvx/UPR-FVX.jar sh /scripts/build-fvx.sh
COPY bridge/src /bridge/src
RUN mkdir -p /bridge/classes /bridge/dist; \
  javac -cp /upr-fvx/UPR-FVX.jar -d /bridge/classes /bridge/src/dev/uprweb/SettingsBridge.java; \
  jar --create --file /bridge/dist/settings-bridge.jar -C /bridge/classes .

FROM node:25-bookworm-slim AS runtime
ENV NODE_ENV=production \
  PORT=3000 \
  UPR_FVX_JAR=/app/vendor/UPR-FVX.jar \
  JAVA_BIN=/app/vendor/java/bin/java
WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends tini \
  && rm -rf /var/lib/apt/lists/*

COPY package.json package-lock.json ./
RUN npm ci --omit=dev \
  && npm cache clean --force

COPY server ./server
COPY README.md ./
COPY --from=build /app/dist ./dist
COPY --from=upr-fvx /upr-fvx/UPR-FVX.jar ./vendor/UPR-FVX.jar
COPY --from=upr-fvx /upr-fvx/data ./data
COPY --from=upr-fvx /upr-fvx/java ./vendor/java
COPY --from=upr-fvx /bridge/dist/settings-bridge.jar ./bridge/dist/settings-bridge.jar

RUN mkdir -p /app/tmp \
  && chown -R node:node /app

USER node
EXPOSE 3000
ENTRYPOINT ["tini", "--"]
CMD ["node", "server/index.js"]
