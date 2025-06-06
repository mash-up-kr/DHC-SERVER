#!/bin/bash
set -euo pipefail

ENVIRONMENT="${1:-staging}"
REGISTRY="${REGISTRY:-$NCP_CONTAINER_REGISTRY}"
IMAGE_NAME="${IMAGE_NAME:-dhc-ktor-app}"
VERSION="${TAG:-${VERSION:-latest}}"
CONTAINER_NAME="dhc-app-${ENVIRONMENT}"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"; }
error() { echo "[ERROR] $1" >&2; exit 1; }

[ -z "${REGISTRY:-}" ] && error "REGISTRY not set"
[ -z "${NCP_ACCESS_KEY:-}" ] && error "NCP_ACCESS_KEY not set"
[ -z "${NCP_SECRET_KEY:-}" ] && error "NCP_SECRET_KEY not set"

log "Logging in to registry..."
echo "${NCP_SECRET_KEY}" | docker login "${REGISTRY}" -u "${NCP_ACCESS_KEY}" --password-stdin

# 이미지 풀
IMAGE="${REGISTRY}/${IMAGE_NAME}:${VERSION}"
log "Pulling image: ${IMAGE}"
docker pull "${IMAGE}"

# 기존 컨테이너 정지 및 제거
docker stop "${CONTAINER_NAME}" 2>/dev/null || true
docker rm "${CONTAINER_NAME}" 2>/dev/null || true

case "${ENVIRONMENT}" in
    "production") ENV_VARS="-e ENVIRONMENT=production -e LOG_LEVEL=INFO" ;;
    "staging") ENV_VARS="-e ENVIRONMENT=staging -e LOG_LEVEL=DEBUG" ;;
    *) ENV_VARS="-e ENVIRONMENT=development -e LOG_LEVEL=DEBUG" ;;
esac

log "컨테이너 시작..."
docker run -d \
    --name "${CONTAINER_NAME}" \
    --restart unless-stopped \
    -p 8080:8080 \
    ${ENV_VARS} \
    -e MONGO_CONNECTION_STRING="${MONGO_CONNECTION_STRING:-}" \
    -e APP_VERSION="${VERSION}" \
    --health-cmd="curl -f http://localhost:8080/health || exit 1" \
    --health-interval=30s \
    --health-retries=3 \
    "${IMAGE}"

log "헬스체크 대기중..."
for i in {1..30}; do
    if [ "$(docker inspect -f '{{.State.Health.Status}}' "${CONTAINER_NAME}" 2>/dev/null)" == "healthy" ]; then
        log "Deployment successful!"
        docker image prune -f
        exit 0
    fi
    sleep 2
done

error "Health check failed"