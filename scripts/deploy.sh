#!/bin/bash
set -euo pipefail

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
docker run -d \
    --name "${CONTAINER_NAME}" \
    --restart unless-stopped \
    -p 8080:8080 \
    -e MONGO_CONNECTION_STRING="${MONGO_CONNECTION_STRING:-}" \
    -e APP_VERSION="${VERSION}" \
    "${IMAGE}"

log "컨테이너 시작 완료"
docker image prune -f