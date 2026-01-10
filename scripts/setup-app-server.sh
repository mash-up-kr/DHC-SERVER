#!/bin/bash

# App 서버 설정 스크립트 (Oracle Linux 8, 1GB RAM)
# Docker 설치 및 swap 설정

set -e

echo "=========================================="
echo "App 서버 설정 시작"
echo "=========================================="

# 1. Swap 설정 (2GB)
echo "[1/4] Swap 설정 중..."
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

# swappiness 설정
sudo sysctl vm.swappiness=60
echo 'vm.swappiness=60' | sudo tee -a /etc/sysctl.conf

# 2. Docker 설치
echo "[2/4] Docker 설치 중..."
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

# 3. 방화벽 설정 (앱 포트)
echo "[3/4] 방화벽 설정 중..."
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --reload
echo "방화벽 설정 완료 (80, 443, 8080 포트 오픈)"

# 4. 작업 디렉토리 생성
echo "[4/4] 작업 디렉토리 생성 중..."
mkdir -p ~/dhc
cd ~/dhc

echo ""
echo "=========================================="
echo "App 서버 설정 완료!"
echo "=========================================="
echo ""
echo "다음 단계:"
echo "1. 새 터미널에서 다시 로그인 (docker 그룹 적용)"
echo "2. .env 파일 생성 후 docker-compose.app.yml로 앱 실행"
echo ""
