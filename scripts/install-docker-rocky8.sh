#!/bin/bash

# Rocky Linux 8.10 Docker 설치 스크립트

set -e  # 오류 발생 시 스크립트 중단

echo "=========================================="
echo "Rocky Linux 8.10 Docker 설치 시작"
echo "=========================================="

# 1. 시스템 업데이트 및 기존 패키지 정리
echo "[1/5] 시스템 패키지 캐시 정리 중..."
sudo dnf clean all

# 2. 필요한 패키지 설치
echo "[2/5] 필수 패키지 설치 중..."
sudo dnf -y install dnf-plugins-core

# 3. Docker 저장소 추가
echo "[3/5] Docker 저장소 추가 중..."
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# 4. docker-ce.repo 파일 수정 (Rocky Linux 8.10 호환성을 위함)
echo "[4/5] Docker 저장소 설정 수정 중..."
sudo sed -i 's|$releasever|8|g' /etc/yum.repos.d/docker-ce.repo

# 5. Docker 패키지 설치
echo "[5/5] Docker 패키지 설치 중..."
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 6. Docker 서비스 활성화 및 시작
echo "Docker 서비스 활성화 및 시작 중..."
sudo systemctl enable --now docker

# 7. 설치 확인
echo ""
echo "=========================================="
echo "Docker 설치 완료"
echo "=========================================="
echo "Docker 버전 정보:"
docker --version
echo ""

# 8. 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER
sudo newgrp docker
echo "docker 그룹에 현재 사용자 추가완료"

echo "Docker 설치완료"