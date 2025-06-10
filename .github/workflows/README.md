## Workflows

### 1. PR Build (`pr-build.yml`)
- **Trigger**: 모든 브랜치의 PR
- **Actions**:
  - 변경사항 감지 (backend/docker)
  - Ktlint 검사 및 테스트
  - JAR 빌드 검증
  - Docker 이미지 검증
  - PR 상태 코멘트

### 2. Release Deploy (`release.yml`)
- **Trigger**: GitHub Release 발행 또는 수동 실행
- **Actions**:
  - 버전별 JAR 빌드
  - Docker 이미지 빌드 및 푸시
  - 서버 자동 배포
  - 헬스체크
  - 릴리즈 노트 업데이트

## Required Secrets

### NCP
- `NCP_CONTAINER_REGISTRY`: Registry URL
- `NCP_ACCESS_KEY`: Access Key
- `NCP_SECRET_KEY`: Secret Key
- `NCP_SERVER_HOST`: 배포 서버
- `NCP_SERVER_USER`: SSH 사용자
- `NCP_SERVER_PASSWORD`: SSH 비밀번호
- `NCP_SERVER_SSH_KEY`: SSH Key

### Application
- `MONGO_CONNECTION_STRING`: MongoDB 연결 문자열