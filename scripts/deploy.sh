#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== DHC 애플리케이션 Docker Compose 배포 ===${NC}"

# 스크립트 디렉토리 확인
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Docker 설치 확인 및 설치
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW} Docker가 설치되어 있지 않습니다. 설치를 시작합니다...${NC}"
    
    # OS 확인
    if [ -f /etc/rocky-release ]; then
        echo -e "${GREEN}Rocky Linux 감지됨. Docker 설치 시작...${NC}"
        chmod +x "$SCRIPT_DIR/install-docker-rocky8.sh"
        "$SCRIPT_DIR/install-docker-rocky8.sh"
        
        # Docker 설치 후 서비스 확인
        if ! systemctl is-active --quiet docker; then
            echo -e "${RED}Docker 서비스가 실행되지 않습니다${NC}"
            exit 1
        fi
    else
        echo -e "${RED}지원하지 않는 OS입니다. 수동으로 Docker를 설치해주세요.${NC}"
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

# docker-compose 파일 확인
if [ ! -f "docker-compose.deploy.yml" ]; then
    echo -e "${RED}docker-compose.deploy.yml 파일을 찾을 수 없습니다${NC}"
    exit 1
fi

# 환경 변수 로드 및 확인
set -a
source .env.production
set +a

if [ -z "$NCP_ACCESS_KEY" ] || [ -z "$NCP_SECRET_KEY" ]; then
    echo -e "${RED}NCP 인증 정보가 설정되지 않았습니다${NC}"
    exit 1
fi

if [ -z "$REGISTRY" ] || [ -z "$IMAGE_TAG" ]; then
    echo -e "${RED}레지스트리 또는 이미지 태그가 설정되지 않았습니다${NC}"
    exit 1
fi

# 1. 레지스트리 로그인
echo -e "${GREEN}레지스트리 로그인 중...${NC}"
echo "$NCP_SECRET_KEY" | docker login -u "$NCP_ACCESS_KEY" --password-stdin "$REGISTRY"

if [ $? -ne 0 ]; then
    echo -e "${RED}레지스트리 로그인 실패${NC}"
    exit 1
fi

# 2. MongoDB 데이터 디렉토리 생성
echo -e "${GREEN}MongoDB 데이터 디렉토리 확인 중...${NC}"
if [ ! -d "/data/mongodb" ]; then
    echo -e "${YELLOW}MongoDB 데이터 디렉토리 생성 중...${NC}"
    sudo mkdir -p /data/mongodb /data/mongodb-config
    sudo chown -R 999:999 /data/mongodb /data/mongodb-config  # MongoDB 컨테이너 사용자 권한
    echo -e "${GREEN}데이터 디렉토리 생성 완료${NC}"
fi

# 2-1. MongoDB 초기화 스크립트 디렉토리 확인
echo -e "${GREEN}MongoDB 초기화 스크립트 확인 중...${NC}"
if [ ! -d "$PROJECT_ROOT/scripts" ]; then
    mkdir -p "$PROJECT_ROOT/scripts"
fi

# 2-2. MongoDB 초기화 스크립트 생성 (없을 경우)
if [ ! -f "$PROJECT_ROOT/scripts/mongo-init.js" ]; then
    echo -e "${YELLOW}MongoDB 초기화 스크립트 생성 중...${NC}"
    cat > "$PROJECT_ROOT/scripts/mongo-init.js" <<'EOF'
// MongoDB 초기화 스크립트
db = db.getSiblingDB('dhc');

// 필요한 컬렉션 생성
db.createCollection('users');
db.createCollection('sessions');
db.createCollection('files');
db.createCollection('logs');

// 인덱스 생성
db.users.createIndex({ "email": 1 }, { unique: true });
db.sessions.createIndex({ "createdAt": 1 }, { expireAfterSeconds: 3600 });
db.files.createIndex({ "uploadedAt": 1 });
db.logs.createIndex({ "timestamp": -1 });

print('Database initialization completed');
EOF
    echo -e "${GREEN}초기화 스크립트 생성 완료${NC}"
fi

# 3. 현재 상태 확인
echo -e "${BLUE}현재 컨테이너 상태:${NC}"
$COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production ps

# 4. 기존 컨테이너 중지 (graceful shutdown)
echo -e "${YELLOW}기존 컨테이너 중지 중...${NC}"
$COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production stop
$COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production rm -f

# 5. 새 이미지 pull
echo -e "${GREEN}최신 이미지 다운로드 중...${NC}"
$COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production pull

if [ $? -ne 0 ]; then
    echo -e "${RED}이미지 다운로드 실패${NC}"
    exit 1
fi

# 6. 컨테이너 시작
echo -e "${GREEN}새 컨테이너 시작 중...${NC}"
$COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}컨테이너 시작 실패${NC}"
    exit 1
fi
# 7. MongoDB Replica Set 초기화
echo -e "${YELLOW}MongoDB Replica Set 초기화 중...${NC}"
bash "$PROJECT_ROOT/scripts/init-replica-set.sh"

# 8. 애플리케이션 시작 대기#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== DHC 애플리케이션 Docker Compose 배포 ===${NC}"

# 스크립트 디렉토리 확인
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Docker 설치 확인 및 설치
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW} Docker가 설치되어 있지 않습니다. 설치를 시작합니다...${NC}"

    # OS 확인
    if [ -f /etc/rocky-release ]; then
        echo -e "${GREEN}Rocky Linux 감지됨. Docker 설치 시작...${NC}"
        chmod +x "$SCRIPT_DIR/install-docker-rocky8.sh"
        "$SCRIPT_DIR/install-docker-rocky8.sh"

        # Docker 설치 후 서비스 확인
        if ! systemctl is-active --quiet docker; then
            echo -e "${RED}Docker 서비스가 실행되지 않습니다${NC}"
            exit 1
        fi
    else
        echo -e "${RED}지원하지 않는 OS입니다. 수동으로 Docker를 설치해주세요.${NC}"
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

# docker-compose 파일 확인
if [ ! -f "docker-compose.deploy.yml" ]; then
    echo -e "${RED}docker-compose.deploy.yml 파일을 찾을 수 없습니다${NC}"
    exit 1
fi

# 환경 변수 로드 및 확인
set -a
source .env.production
set +a

if [ -z "$NCP_ACCESS_KEY" ] || [ -z "$NCP_SECRET_KEY" ]; then
    echo -e "${RED}NCP 인증 정보가 설정되지 않았습니다${NC}"
    exit 1
fi

if [ -z "$REGISTRY" ] || [ -z "$IMAGE_TAG" ]; then
    echo -e "${RED}레지스트리 또는 이미지 태그가 설정되지 않았습니다${NC}"
    exit 1
fi

# 1. 레지스트리 로그인
echo -e "${GREEN}레지스트리 로그인 중...${NC}"
echo "$NCP_SECRET_KEY" | docker login -u "$NCP_ACCESS_KEY" --password-stdin "$REGISTRY"

if [ $? -ne 0 ]; then
    echo -e "${RED}레지스트리 로그인 실패${NC}"
    exit 1
fi

# 2. MongoDB 데이터 디렉토리 생성
echo -e "${GREEN}MongoDB 데이터 디렉토리 확인 중...${NC}"
if [ ! -d "/data/mongodb" ]; then
    echo -e "${YELLOW}MongoDB 데이터 디렉토리 생성 중...${NC}"
    sudo mkdir -p /data/mongodb /data/mongodb-config
    sudo chown -R 999:999 /data/mongodb /data/mongodb-config  # MongoDB 컨테이너 사용자 권한
    echo -e "${GREEN}데이터 디렉토리 생성 완료${NC}"
fi

# 2-1. MongoDB Replica Set 초기화 스크립트 생성
echo -e "${GREEN}MongoDB Replica Set 초기화 스크립트 확인 중...${NC}"
if [ ! -f "$PROJECT_ROOT/scripts/init-replica-set.sh" ]; then
    echo -e "${YELLOW}Replica Set 초기화 스크립트 생성 중...${NC}"
    cat > "$PROJECT_ROOT/scripts/init-replica-set.sh" <<'EOF'
#!/bin/bash

echo "MongoDB Replica Set 초기화 시작..."

# MongoDB가 준비될 때까지 대기
max_retries=30
retry_count=0

while [ $retry_count -lt $max_retries ]; do
    if docker exec mongodb mongosh --eval "db.adminCommand('ping')" &>/dev/null; then
        echo "MongoDB가 준비되었습니다."
        break
    fi
    retry_count=$((retry_count + 1))
    echo "MongoDB 대기 중... ($retry_count/$max_retries)"
    sleep 2
done

if [ $retry_count -eq $max_retries ]; then
    echo "MongoDB 시작 실패"
    exit 1
fi

# Replica Set 초기화
echo "Replica Set 초기화 중..."
docker exec mongodb mongosh --eval "
try {
    rs.status();
    print('Replica Set이 이미 초기화되어 있습니다.');
} catch (e) {
    print('Replica Set 초기화 중...');
    rs.initiate({
        _id: 'rs0',
        members: [{
            _id: 0,
            host: 'localhost:27017'
        }]
    });
    print('Replica Set 초기화 완료');
}
"

echo "MongoDB Replica Set 초기화 완료"
EOF
    chmod +x "$PROJECT_ROOT/scripts/init-replica-set.sh"
    echo -e "${GREEN}초기화 스크립트 생성 완료${NC}"
fi

# 3. 현재 상태 확인
echo -e "${BLUE}현재 컨테이너 상태:${NC}"
$COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production ps

# 4. 기존 컨테이너 중지 (graceful shutdown)
echo -e "${YELLOW}기존 컨테이너 중지 중...${NC}"
$COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production stop
$COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production rm -f

# 5. 새 이미지 pull
echo -e "${GREEN}최신 이미지 다운로드 중...${NC}"
$COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production pull

if [ $? -ne 0 ]; then
    echo -e "${RED}이미지 다운로드 실패${NC}"
    exit 1
fi

# 6. 컨테이너 시작
echo -e "${GREEN}새 컨테이너 시작 중...${NC}"
$COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}컨테이너 시작 실패${NC}"
    exit 1
fi

# 7. MongoDB Replica Set 초기화
echo -e "${YELLOW}MongoDB Replica Set 초기화 중...${NC}"
bash "$PROJECT_ROOT/scripts/init-replica-set.sh"

# 8. 애플리케이션 시작 대기
echo -e "${YELLOW}애플리케이션 시작 대기 중...${NC}"
sleep 10

# 9. 상태 확인
echo -e "${BLUE}배포 후 컨테이너 상태:${NC}"
$COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production ps

# 10. 헬스체크
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
    $COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production logs dhc-app --tail=50
    exit 1
fi

# 11. MongoDB 상태 확인
echo -e "${BLUE}MongoDB Replica Set 상태:${NC}"
docker exec mongodb mongosh --quiet --eval "rs.status().members.forEach(m => print(m.name + ': ' + m.stateStr))"

# 12. 로그 확인
echo -e "${BLUE}최근 로그:${NC}"
$COMPOSE_CMD -f docker-compose.deploy.yml --env-file .env.production logs --tail=20

# 13. 이전 이미지 정리
echo -e "${YELLOW}사용하지 않는 이미지 정리 중...${NC}"
docker image prune -f

echo -e "${GREEN}배포 완료${NC}"
echo -e "${BLUE}서비스 상태 확인: ${COMPOSE_CMD} -f docker-compose.deploy.yml --env-file .env.production ps${NC}"
echo -e "${BLUE}로그 확인: ${COMPOSE_CMD} -f docker-compose.deploy.yml --env-file .env.production logs -f${NC}"
echo -e "${BLUE}MongoDB 상태: docker exec mongodb mongosh --eval 'rs.status()'${NC}"