# GitHub Actions CI/CD Pipelines

## Pipelines

### 1. CI Pipeline (`ci.yml`)
- **Trigger**: main 브랜치 push 또는 수동 실행
- **Actions**: 
  - 테스트
  - JAR 빌드
  - Docker 이미지 빌드 및 NCP Registry 푸시
  - 태그: `latest`, `main`, `main-{sha}`, `YYYYMMDD-HHmmss`

### 2. PR Build (`pr-build.yml`)
- **Trigger**: main/develop 브랜치 대상 PR
- **Actions**:
  - 변경사항 감지
  - Ktlint 및 테스트
  - 빌드 검증
  - Docker 빌드 검증 (푸시 없음)
  - PR 결과 코멘트

### 3. Release Deploy (`release-deploy.yml`)
- **Trigger**: GitHub Release 발행
- **Actions**:
  - 릴리즈 버전 빌드
  - 버전 태그로 이미지 푸시
  - 프로덕션 자동 배포
  - 헬스체크
  - 릴리즈 노트 업데이트

## GitHub Secrets

### NCP Registry
- `NCP_CONTAINER_REGISTRY`: Registry URL (예: kr.ncr.ntruss.com)
- `NCP_ACCESS_KEY`: NCP Access Key
- `NCP_SECRET_KEY`: NCP Secret Key

### Server Access
- `NCP_SERVER_HOST`: 배포 서버 주소
- `NCP_SERVER_USER`: SSH 사용자 (기본: root)
- `NCP_SERVER_SSH_KEY`: SSH Private Key

### Application
- `MONGO_CONNECTION_STRING`: MongoDB 연결 문자열

### Optional
- `SLACK_WEBHOOK_URL`: Slack 알림용
- `CODECOV_TOKEN`: 코드 커버리지용

## Usage

### Local Build & Push
```bash
# NCP Registry 로그인
docker login kr.ncr.ntruss.com -u <ACCESS_KEY> -p <SECRET_KEY>

# 이미지 빌드
docker build -t kr.ncr.ntruss.com/dhc-ktor-app:local .

# 이미지 푸시
docker push kr.ncr.ntruss.com/dhc-ktor-app:local
```