#!/bin/bash

# Oracle Linux 8/9용 Docker 설치 스크립트

set -e

echo "=========================================="
echo "Oracle Linux Docker 설치 스크립트"
echo "=========================================="

# Root 권한 확인
if [ "$EUID" -ne 0 ]; then
    echo "이 스크립트는 root 권한으로 실행해야 합니다."
    echo "sudo $0 또는 root 사용자로 실행하세요."
    exit 1
fi

# OS 버전 확인
if [ -f /etc/oracle-release ]; then
    OS_VERSION=$(cat /etc/oracle-release)
    echo "감지된 OS: $OS_VERSION"
else
    echo "Oracle Linux가 아닙니다."
    exit 1
fi

# 기존 Docker 관련 패키지 제거
echo "기존 Docker 패키지 제거 중..."
dnf remove -y docker \
    docker-client \
    docker-client-latest \
    docker-common \
    docker-latest \
    docker-latest-logrotate \
    docker-logrotate \
    docker-engine \
    podman \
    runc 2>/dev/null || true

# 필수 패키지 설치
echo "필수 패키지 설치 중..."
dnf install -y dnf-utils

# Docker CE 리포지토리 추가
echo "Docker CE 리포지토리 추가 중..."
dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# Docker 설치
echo "Docker CE 설치 중..."
dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Docker 서비스 시작 및 활성화
echo "Docker 서비스 시작 중..."
systemctl start docker
systemctl enable docker

# Docker 버전 확인
echo "Docker 버전 확인..."
docker --version
docker compose version

# opc 사용자를 docker 그룹에 추가 (OCI 기본 사용자)
if id "opc" &>/dev/null; then
    echo "opc 사용자를 docker 그룹에 추가 중..."
    usermod -aG docker opc
fi

# 현재 사용자를 docker 그룹에 추가
if [ -n "$SUDO_USER" ]; then
    echo "$SUDO_USER 사용자를 docker 그룹에 추가 중..."
    usermod -aG docker "$SUDO_USER"
fi

# Docker 상태 확인
echo "Docker 상태 확인..."
systemctl status docker --no-pager

echo ""
echo "=========================================="
echo "Docker 설치 완료!"
echo "=========================================="
echo ""
echo "참고: docker 명령어를 sudo 없이 사용하려면"
echo "다시 로그인하거나 다음 명령어를 실행하세요:"
echo "  newgrp docker"
echo ""
