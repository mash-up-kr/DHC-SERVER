#!/bin/bash
#
# Rocky Linux 8.10 커널 튜닝 스크립트
# 참고: Red Hat Enterprise Linux 8 Performance Tuning Guide
# https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/8/html/monitoring_and_managing_system_status_and_performance/index

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_root() {
    if [[ $EUID -ne 0 ]]; then
        log_error "root 권한 필요"
        exit 1
    fi
}

check_rocky_version() {
    if [[ -f /etc/rocky-release ]]; then
        VERSION=$(cat /etc/rocky-release | grep -oP '(?<=release )\d+\.\d+')
        if [[ "$VERSION" != "8.10" ]]; then
            log_warn "현재 OS 버전: $VERSION"
        fi
    else
        log_error "???"
        exit 1
    fi
}

tune_network() {
    log_info "네트워크 성능 튜닝 적용 중..."
    
    cat > /etc/sysctl.d/99-network-tuning.conf << EOF
# Network Performance Tuning for Rocky Linux 8.10

# TCP 버퍼 크기 증가
net.core.rmem_default = 262144
net.core.rmem_max = 16777216
net.core.wmem_default = 262144
net.core.wmem_max = 16777216

# TCP 자동 튜닝
net.ipv4.tcp_rmem = 4096 87380 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216

# 네트워크 백로그 증가
net.core.netdev_max_backlog = 5000
net.ipv4.tcp_max_syn_backlog = 8192

# TCP 연결 추적 테이블 크기 증가
net.netfilter.nf_conntrack_max = 524288
net.netfilter.nf_conntrack_tcp_timeout_established = 1800

# TCP Fast Open 활성화
net.ipv4.tcp_fastopen = 3

# TCP 혼잡 제어 알고리즘 (BBR)
net.core.default_qdisc = fq
net.ipv4.tcp_congestion_control = bbr

# TIME_WAIT 소켓 재사용
net.ipv4.tcp_tw_reuse = 1

# TCP keepalive 시간 단축
net.ipv4.tcp_keepalive_time = 300
net.ipv4.tcp_keepalive_probes = 5
net.ipv4.tcp_keepalive_intvl = 30

# IP 로컬 포트 범위 확장
net.ipv4.ip_local_port_range = 10000 65535

# SYN 플러드 공격 방어
net.ipv4.tcp_syncookies = 1
net.ipv4.tcp_max_tw_buckets = 2000000

# TCP 재전송 설정
net.ipv4.tcp_retries2 = 8
EOF
}

tune_memory() {
    log_info "메모리 성능 튜닝 적용 중..."
    
    cat > /etc/sysctl.d/99-memory-tuning.conf << EOF
# Memory Performance Tuning for Rocky Linux 8.10

# 가상 메모리 설정
vm.swappiness = 10
vm.dirty_ratio = 15
vm.dirty_background_ratio = 5
vm.vfs_cache_pressure = 50

# 공유 메모리 설정
kernel.shmmax = 68719476736
kernel.shmall = 4294967296
kernel.shmmni = 4096

# 메모리 오버커밋 설정
vm.overcommit_memory = 1
vm.overcommit_ratio = 50

# Zone reclaim 모드 비활성화 (NUMA 시스템)
vm.zone_reclaim_mode = 0

# 페이지 캐시 설정
vm.min_free_kbytes = 65536
EOF
}

tune_filesystem() {
    log_info "파일 시스템 튜닝 적용 중..."
    
    cat > /etc/sysctl.d/99-filesystem-tuning.conf << EOF
# Filesystem Performance Tuning for Rocky Linux 8.10

# 최대 파일 핸들 수 증가
fs.file-max = 2097152

# inotify 설정
fs.inotify.max_user_watches = 524288
fs.inotify.max_user_instances = 512

# AIO 요청 수 증가
fs.aio-max-nr = 1048576

# 파일 시스템 캐시 압력
vm.vfs_cache_pressure = 50
EOF
    
    # limits.conf 설정
    cat > /etc/security/limits.d/99-tuning.conf << EOF
# 파일 디스크립터 제한 증가
* soft nofile 65536
* hard nofile 131072
* soft nproc 65536
* hard nproc 131072

# 메모리 잠금 제한
* soft memlock unlimited
* hard memlock unlimited
EOF
}

tune_cpu() {
    log_info "CPU 성능 튜닝 적용 중..."
    
    # cpupower 도구 설치 확인
    if ! command -v cpupower &> /dev/null; then
        log_info "cpupower 설치 중..."
        dnf install -y kernel-tools &> /dev/null
    fi
    
    # CPU 거버너를 performance로 설정
    if command -v cpupower &> /dev/null; then
        cpupower frequency-set -g performance &> /dev/null || true
        
        # 부팅 시 자동 적용
        cat > /etc/systemd/system/cpu-performance.service << EOF
[Unit]
Description=Set CPU Governor to Performance
After=multi-user.target

[Service]
Type=oneshot
ExecStart=/usr/bin/cpupower frequency-set -g performance
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
EOF
        
        systemctl daemon-reload
        systemctl enable cpu-performance.service &> /dev/null
    fi
    
    # IRQ 밸런싱 설정
    cat > /etc/sysctl.d/99-cpu-tuning.conf << EOF
# CPU Performance Tuning for Rocky Linux 8.10

# 프로세스 스케줄링 설정
kernel.sched_migration_cost_ns = 5000000
kernel.sched_autogroup_enabled = 0

# NUMA 밸런싱
kernel.numa_balancing = 1
EOF
}

tune_security() {
    log_info "보안 관련 커널 파라미터 적용 중..."
    
    cat > /etc/sysctl.d/99-security-tuning.conf << EOF
# Security Kernel Parameters for Rocky Linux 8.10

# IP 스푸핑 방지
net.ipv4.conf.all.rp_filter = 1
net.ipv4.conf.default.rp_filter = 1

# IP 소스 라우팅 비활성화
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.default.accept_source_route = 0

# ICMP 리다이렉트 비활성화
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
net.ipv4.conf.all.secure_redirects = 0
net.ipv4.conf.default.secure_redirects = 0

# 패킷 포워딩 비활성화
net.ipv4.ip_forward = 0
net.ipv6.conf.all.forwarding = 0

# 로그 마티언 패킷
net.ipv4.conf.all.log_martians = 1
net.ipv4.conf.default.log_martians = 1

# 코어 덤프 제한
kernel.core_uses_pid = 1
fs.suid_dumpable = 0

# ExecShield 보호
kernel.randomize_va_space = 2

# 하드 링크/심볼릭 링크 보호
fs.protected_hardlinks = 1
fs.protected_symlinks = 1
EOF
}

configure_tuned() {
    log_info "tuned 프로파일 설정 중..."
    
    # tuned 설치 및 시작
    if ! systemctl is-active --quiet tuned; then
        systemctl start tuned &> /dev/null
        systemctl enable tuned &> /dev/null
    fi
    
    # throughput-performance 프로파일 적용
    tuned-adm profile throughput-performance &> /dev/null
    log_info "tuned 프로파일을 'throughput-performance'로 설정"
}

apply_settings() {
    log_info "커널 파라미터 적용 중..."
    sysctl -p /etc/sysctl.d/99-network-tuning.conf &> /dev/null
    sysctl -p /etc/sysctl.d/99-memory-tuning.conf &> /dev/null
    sysctl -p /etc/sysctl.d/99-filesystem-tuning.conf &> /dev/null
    sysctl -p /etc/sysctl.d/99-cpu-tuning.conf &> /dev/null
    sysctl -p /etc/sysctl.d/99-security-tuning.conf &> /dev/null
}

verify_settings() {
    echo ""
    echo "=== 적용된 주요 설정 ==="
    echo "네트워크 버퍼: $(sysctl -n net.core.rmem_max 2>/dev/null) bytes"
    echo "TCP 혼잡 제어: $(sysctl -n net.ipv4.tcp_congestion_control 2>/dev/null)"
    echo "Swappiness: $(sysctl -n vm.swappiness 2>/dev/null)"
    echo "파일 핸들 최대값: $(sysctl -n fs.file-max 2>/dev/null)"
    
    if command -v tuned-adm &> /dev/null; then
        echo "Tuned 프로파일: $(tuned-adm active 2>/dev/null | grep -oP '(?<=Current active profile: ).*')"
    fi
    
    if command -v cpupower &> /dev/null; then
        GOV=$(cpupower frequency-info -p 2>/dev/null | grep -oP '(?<=governor ").*(?=")')
        [ -n "$GOV" ] && echo "CPU 거버너: $GOV"
    fi
    echo ""
}

# 메인 실행
main() {
    echo "================================================"
    echo "Rocky Linux 8.10 커널 튜닝 스크립트"
    echo "================================================"
    
    # 사전 체크
    check_root
    check_rocky_version
    
    # 모든 튜닝 자동 적용
    tune_network
    tune_memory
    tune_filesystem
    tune_cpu
    tune_security
    configure_tuned
    
    # 설정 적용
    apply_settings
    
    # 검증
    verify_settings
    
    log_info "커널 튜닝이 완료"

    read -p "재부팅 ㄱ? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        reboot
    fi
}

# 스크립트 실행
main "$@"
