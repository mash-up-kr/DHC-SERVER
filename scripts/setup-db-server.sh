#!/bin/bash

# DB 서버 설정 스크립트 (Oracle Linux 8, 1GB RAM)
# MongoDB + Docker 설치 및 swap 설정

set -e

echo "=========================================="
echo "DB 서버 설정 시작"
echo "=========================================="

# 1. Swap 설정 (2GB)
echo "[1/5] Swap 설정 중..."
if [ ! -f /swapfile ]; then
    sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
    echo "Swap 설정 완료 (2GB)"
else
    echo "Swap 이미 존재함"
fi

# swappiness 설정 (메모리 부족 시 swap 사용)
sudo sysctl vm.swappiness=60
echo 'vm.swappiness=60' | sudo tee -a /etc/sysctl.conf

# 2. Docker 설치
echo "[2/5] Docker 설치 중..."
if ! command -v docker &> /dev/null; then
    sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
    sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    sudo systemctl enable docker
    sudo systemctl start docker
    sudo usermod -aG docker opc
    echo "Docker 설치 완료"
else
    echo "Docker 이미 설치됨"
fi

# 3. 방화벽 설정 (MongoDB 포트)
echo "[3/5] 방화벽 설정 중..."
sudo firewall-cmd --permanent --add-port=27017/tcp
sudo firewall-cmd --reload
echo "방화벽 설정 완료 (27017 포트 오픈)"

# 4. 작업 디렉토리 생성
echo "[4/5] 작업 디렉토리 생성 중..."
mkdir -p ~/dhc
cd ~/dhc

# 5. docker-compose.db.yml 생성
echo "[5/5] docker-compose 파일 생성 중..."
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  mongodb:
    image: mongo:latest
    container_name: mongodb
    network_mode: host
    environment:
      MONGO_INITDB_DATABASE: dhc
      TZ: Asia/Seoul
    volumes:
      - mongodb_data:/data/db
      - mongodb_config:/data/configdb
    command: mongod --replSet rs0 --bind_ip_all --wiredTigerCacheSizeGB 0.25
    healthcheck:
      test: [ "CMD", "mongosh", "--eval", "db.adminCommand('ping')" ]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 40s
    restart: unless-stopped
    mem_limit: 768m
    memswap_limit: 1536m

volumes:
  mongodb_data:
  mongodb_config:
EOF

echo ""
echo "=========================================="
echo "DB 서버 설정 완료!"
echo "=========================================="
echo ""
echo "다음 단계:"
echo "1. 새 터미널에서 다시 로그인 (docker 그룹 적용)"
echo "2. MongoDB 시작: cd ~/dhc && docker compose up -d"
echo "3. Replica Set 초기화:"
echo "   docker exec -it mongodb mongosh --eval \"rs.initiate()\""
echo ""
