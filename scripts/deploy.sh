#!/bin/bash
set -euo pipefail

ENV_FILE="/etc/dhc-app/.env"

if [ -f "$ENV_FILE" ]; then
    log "Loading environment from $ENV_FILE"
    set -a
    source "$ENV_FILE"
    set +a
else
    log "$ENV_FILE not found"
fi

REGISTRY="${REGISTRY:-$NCP_CONTAINER_REGISTRY}"
IMAGE_NAME="${IMAGE_NAME:-dhc-ktor-app}"
VERSION="${TAG:-${VERSION:-latest}}"
CONTAINER_NAME="dhc-app"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"; }
error() { echo "[ERROR] $1" >&2; exit 1; }

[ -z "${REGISTRY:-}" ] && error "REGISTRY not set"
[ -z "${NCP_ACCESS_KEY:-}" ] && error "NCP_ACCESS_KEY not set"
[ -z "${NCP_SECRET_KEY:-}" ] && error "NCP_SECRET_KEY not set"

log "Logging in to registry..."
echo "${NCP_SECRET_KEY}" | docker login -u "${NCP_ACCESS_KEY}" --password-stdin "${REGISTRY}"

# 이미지 풀
IMAGE="${REGISTRY}/${IMAGE_NAME}:${VERSION}"
log "Pulling image: ${IMAGE}"
docker pull "${IMAGE}"

# 기존 컨테이너 정지 및 제거
docker stop "${CONTAINER_NAME}" 2>/dev/null || true
docker rm "${CONTAINER_NAME}" 2>/dev/null || true

log "컨테이너 시작..."
if [ -f "$ENV_FILE" ]; then
    docker run -d \
        --name "${CONTAINER_NAME}" \
        --restart unless-stopped \
        -p 8080:8080 \
        --env-file "$ENV_FILE" \
        -e APP_VERSION="${VERSION}" \
        "${IMAGE}"
else
    # .env 파일이 없을 때
    docker run -d \
        --name "${CONTAINER_NAME}" \
        --restart unless-stopped \
        -p 8080:8080 \
        -e NCP_ACCESS_KEY="${NCP_ACCESS_KEY}" \
        -e NCP_SECRET_KEY="${NCP_SECRET_KEY}" \
        -e BUCKET_NAME="${BUCKET_NAME}" \
        -e MONGO_CONNECTION_STRING="${MONGO_CONNECTION_STRING:-mongodb://localhost:27017/test}" \
        -e APP_VERSION="${VERSION}" \
        "${IMAGE}"
fi

log "컨테이너 시작 완료"
docker image prune -f