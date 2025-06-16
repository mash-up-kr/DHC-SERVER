#!/bin/bash
set -euo pipefail

ENV_FILE="/etc/dhc-app/.env"

# 로깅 함수
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"; }
error() { echo "[ERROR] $1" >&2; exit 1; }

# 환경변수 파일 로드
if [ -f "$ENV_FILE" ]; then
    log "Loading environment from $ENV_FILE"
    set -a
    source "$ENV_FILE"
    set +a
else
    log "$ENV_FILE not found, using environment variables"
fi

# 필수 환경변수 설정
REGISTRY="${REGISTRY:-$NCP_CONTAINER_REGISTRY}"
IMAGE_NAME="${IMAGE_NAME:-dhc-ktor-app}"
VERSION="${TAG:-${VERSION:-latest}}"
CONTAINER_NAME="dhc-app"

# 필수 환경변수 검증
[ -z "${REGISTRY:-}" ] && error "REGISTRY not set"
[ -z "${NCP_ACCESS_KEY:-}" ] && error "NCP_ACCESS_KEY not set"
[ -z "${NCP_SECRET_KEY:-}" ] && error "NCP_SECRET_KEY not set"
[ -z "${BUCKET_NAME:-}" ] && error "BUCKET_NAME not set"

# MongoDB 설정
MONGO_HOST="${MONGO_HOST:-mongo}"
MONGO_PORT="${MONGO_PORT:-27017}"
MONGO_DATABASE="${MONGO_DATABASE:-dhc}"

# Docker 레지스트리 로그인
log "Logging in to registry..."
echo "${NCP_SECRET_KEY}" | docker login -u "${NCP_ACCESS_KEY}" --password-stdin "${REGISTRY}"

# 이미지 풀
IMAGE="${REGISTRY}/${IMAGE_NAME}:${VERSION}"
log "Pulling image: ${IMAGE}"
docker pull "${IMAGE}"

# 기존 컨테이너 백업
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    log "Backing up existing container..."
    docker stop "${CONTAINER_NAME}" 2>/dev/null || true
    docker rename "${CONTAINER_NAME}" "${CONTAINER_NAME}-backup-$(date +%Y%m%d-%H%M%S)" 2>/dev/null || true
fi

# 새 컨테이너 시작
log "Starting new container..."
docker run -d \
    --name "${CONTAINER_NAME}" \
    --restart unless-stopped \
    -p 8080:8080 \
    -e NCP_ACCESS_KEY="${NCP_ACCESS_KEY}" \
    -e NCP_SECRET_KEY="${NCP_SECRET_KEY}" \
    -e BUCKET_NAME="${BUCKET_NAME}" \
    -e MONGO_HOST="${MONGO_HOST}" \
    -e MONGO_PORT="${MONGO_PORT}" \
    -e MONGO_DATABASE="${MONGO_DATABASE}" \
    -e MONGO_CONNECTION_STRING="${MONGO_CONNECTION_STRING:-mongodb://${MONGO_HOST}:${MONGO_PORT}/${MONGO_DATABASE}}" \
    -e APP_VERSION="${VERSION}" \
    "${IMAGE}"

# 헬스체크
log "Performing health check..."
HEALTH_CHECK_URL="http://localhost:8080/health"
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f -s "${HEALTH_CHECK_URL}" >/dev/null 2>&1; then
        log "Health check passed!"

        # 백업 컨테이너 정리
        docker rm $(docker ps -aq -f name="${CONTAINER_NAME}-backup-*") 2>/dev/null || true

        log "Deployment completed successfully!"
        docker image prune -f
        exit 0
    fi

    RETRY_COUNT=$((RETRY_COUNT + 1))
    log "Health check retry ${RETRY_COUNT}/${MAX_RETRIES}"
    sleep 2
done

# 헬스체크 실패 시 롤백
error "Health check failed! Rolling back..."
docker stop "${CONTAINER_NAME}" 2>/dev/null || true
docker rm "${CONTAINER_NAME}" 2>/dev/null || true

# 가장 최근 백업 복원
LATEST_BACKUP=$(docker ps -a --format '{{.Names}}' | grep "${CONTAINER_NAME}-backup-" | sort -r | head -1)
if [ -n "${LATEST_BACKUP}" ]; then
    docker rename "${LATEST_BACKUP}" "${CONTAINER_NAME}"
    docker start "${CONTAINER_NAME}"
    log "Rolled back to ${LATEST_BACKUP}"
fi

exit 1