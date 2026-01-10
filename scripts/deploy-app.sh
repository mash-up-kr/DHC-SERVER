#!/bin/bash

# App 서버 전용 배포 스크립트 (2서버 아키텍처)
# MongoDB는 별도 DB 서버에서 실행 중

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== DHC App 서버 배포 (2서버 아키텍처) ===${NC}"

# 스크립트 디렉토리 확인
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Docker 설치 확인 및 설치
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}Docker가 설치되어 있지 않습니다. 설치를 시작합니다...${NC}"

    # OS 확인
    if [ -f /etc/oracle-release ]; then
        echo -e "${GREEN}Oracle Linux 감지됨. Docker 설치 시작...${NC}"
        chmod +x "$SCRIPT_DIR/install-docker-oracle-linux.sh"
        "$SCRIPT_DIR/install-docker-oracle-linux.sh"
    elif [ -f /etc/rocky-release ]; then
        echo -e "${GREEN}Rocky Linux 감지됨. Docker 설치 시작...${NC}"
        chmod +x "$SCRIPT_DIR/install-docker-rocky8.sh"
        "$SCRIPT_DIR/install-docker-rocky8.sh"
    else
        echo -e "${RED}지원하지 않는 OS입니다. 수동으로 Docker를 설치해주세요.${NC}"
        exit 1
    fi

    # Docker 설치 후 서비스 확인
    if ! systemctl is-active --quiet docker; then
        echo -e "${RED}Docker 서비스가 실행되지 않습니다${NC}"
        exit 1
    fi
fi

# Docker Compose 명령어 확인
if command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
    echo -e "${YELLOW}Docker Compose를 사용합니다${NC}"
elif docker compose version &> /dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
    echo -e "${YELLOW}Docker Compose (플러그인)를 사용합니다${NC}"
else
    echo -e "${RED}Docker Compose가 설치되지 않았습니다${NC}"
    exit 1
fi

# 배포 디렉토리로 이동
cd "$PROJECT_ROOT"

# 환경 파일 확인
if [ ! -f ".env.production" ]; then
    echo -e "${RED}.env.production 파일을 찾을 수 없습니다${NC}"
    exit 1
fi

# docker-compose.app.yml 파일 확인
if [ ! -f "docker-compose.app.yml" ]; then
    echo -e "${RED}docker-compose.app.yml 파일을 찾을 수 없습니다${NC}"
    exit 1
fi

# 환경 변수 로드 및 확인
set -a
source .env.production
set +a

# OCI 인증 정보 확인
if [ -z "$OCI_ACCESS_KEY" ] || [ -z "$OCI_SECRET_KEY" ]; then
    echo -e "${RED}OCI 인증 정보가 설정되지 않았습니다${NC}"
    exit 1
fi

if [ -z "$REGISTRY" ] || [ -z "$IMAGE_TAG" ]; then
    echo -e "${RED}레지스트리 또는 이미지 태그가 설정되지 않았습니다${NC}"
    exit 1
fi

# DB 서버 IP 확인
if [ -z "$DB_SERVER_IP" ]; then
    echo -e "${RED}DB_SERVER_IP가 설정되지 않았습니다${NC}"
    exit 1
fi

echo -e "${GREEN}DB 서버 IP: ${DB_SERVER_IP}${NC}"

# 1. OCI Container Registry 로그인
echo -e "${GREEN}OCI Container Registry 로그인 중...${NC}"
echo "$OCI_SECRET_KEY" | docker login "${OCI_REGION}.ocir.io" -u "${OCI_NAMESPACE}/${OCI_ACCESS_KEY}" --password-stdin

if [ $? -ne 0 ]; then
    echo -e "${RED}레지스트리 로그인 실패${NC}"
    exit 1
fi

# 2. 현재 상태 확인
echo -e "${BLUE}현재 컨테이너 상태:${NC}"
$COMPOSE_CMD -f docker-compose.app.yml --env-file .env.production ps

# 3. 기존 컨테이너 중지 (graceful shutdown)
echo -e "${YELLOW}기존 컨테이너 중지 중...${NC}"
$COMPOSE_CMD -f docker-compose.app.yml --env-file .env.production stop
$COMPOSE_CMD -f docker-compose.app.yml --env-file .env.production rm -f

# 4. 새 이미지 pull
echo -e "${GREEN}최신 이미지 다운로드 중...${NC}"
$COMPOSE_CMD -f docker-compose.app.yml --env-file .env.production pull

if [ $? -ne 0 ]; then
    echo -e "${RED}이미지 다운로드 실패${NC}"
    exit 1
fi

# 5. 컨테이너 시작
echo -e "${GREEN}새 컨테이너 시작 중...${NC}"
$COMPOSE_CMD -f docker-compose.app.yml --env-file .env.production up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}컨테이너 시작 실패${NC}"
    exit 1
fi

# 6. 애플리케이션 시작 대기
echo -e "${YELLOW}애플리케이션 시작 대기 중...${NC}"
sleep 10

# 7. 상태 확인
echo -e "${BLUE}배포 후 컨테이너 상태:${NC}"
$COMPOSE_CMD -f docker-compose.app.yml --env-file .env.production ps

# 8. 헬스체크
echo -e "${GREEN}헬스체크 수행 중...${NC}"
max_retries=10
retry_count=0

while [ $retry_count -lt $max_retries ]; do
    if curl -f -s -X GET http://localhost:8080/health > /dev/null; then
        echo -e "${GREEN}헬스체크 성공!${NC}"
        break
    fi

    retry_count=$((retry_count + 1))
    echo -e "${YELLOW}헬스체크 재시도 $retry_count/$max_retries${NC}"
    sleep 3
done

if [ $retry_count -eq $max_retries ]; then
    echo -e "${RED}헬스체크 실패${NC}"
    echo -e "${RED}애플리케이션 로그:${NC}"
    $COMPOSE_CMD -f docker-compose.app.yml --env-file .env.production logs dhc-app --tail=50
    exit 1
fi

# 9. 로그 확인
echo -e "${BLUE}최근 로그:${NC}"
$COMPOSE_CMD -f docker-compose.app.yml --env-file .env.production logs --tail=20

# 10. 이전 이미지 정리
echo -e "${YELLOW}사용하지 않는 이미지 정리 중...${NC}"
docker image prune -f

echo -e "${GREEN}배포 완료${NC}"
echo -e "${BLUE}서비스 상태 확인: ${COMPOSE_CMD} -f docker-compose.app.yml --env-file .env.production ps${NC}"
echo -e "${BLUE}로그 확인: ${COMPOSE_CMD} -f docker-compose.app.yml --env-file .env.production logs -f${NC}"
