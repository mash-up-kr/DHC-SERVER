# DHC Terraform Infrastructure (OCI)

Oracle Cloud Infrastructure (OCI) Free Tier 기반 인프라 구성

## 인프라 구성

| 구성요소 | 설명 |
|---------|------|
| **Cloud** | Oracle Cloud Infrastructure (OCI) Free Tier |
| **Region** | ap-chuncheon-1 (춘천) |
| **Compute** | VM.Standard.E2.1.Micro (AMD, 1 OCPU, 1GB RAM) x 2 |
| **Network** | VCN + Public Subnet + Security List |
| **Storage** | Object Storage (Standard Tier) |
| **DNS** | DuckDNS (무료) |
| **HTTPS** | Caddy (자동 Let's Encrypt SSL) |

## 서버 구성

```
┌─────────────────────────────────────────────────────────┐
│                    OCI Free Tier                        │
├─────────────────────┬───────────────────────────────────┤
│   App Server        │   DB Server                       │
│   (dhc-app)         │   (dhc-db)                        │
├─────────────────────┼───────────────────────────────────┤
│   - Caddy (443)     │   - MongoDB (27017)               │
│   - Ktor App (8080) │                                   │
└─────────────────────┴───────────────────────────────────┘
```

## 접속 정보

- **API**: https://dhc-2.duckdns.org
- **Swagger**: https://dhc-2.duckdns.org/swagger
- **SSH**: `ssh -i modules/compute/dhc-app-key.pem opc@<PUBLIC_IP>`

---

## 디렉토리 구조

```text
├── modules/
│   ├── compute/              # Compute Instance 모듈
│   ├── vcn/                  # Virtual Cloud Network 모듈
│   ├── subnet/               # Subnet 모듈
│   ├── security_list/        # Security List 모듈 (방화벽)
│   └── object_storage/       # Object Storage 모듈
├── main.tf                   # 루트 모듈
├── variables.tf              # 변수 정의
├── outputs.tf                # 출력값 정의
├── provider.tf               # OCI Provider 설정
└── terraform.tfvars          # 변수 값 (git ignore)
```

---

## DuckDNS 설정

무료 동적 DNS 서비스로 도메인 관리

```bash
# IP 업데이트 (App 서버 IP 변경 시)
curl "https://www.duckdns.org/update?domains=dhc-2&token=<TOKEN>&ip=<NEW_IP>"
```

## Caddy 설정 (App 서버)

자동 HTTPS 리버스 프록시

```bash
# Caddyfile 위치: /etc/caddy/Caddyfile
dhc-2.duckdns.org {
    reverse_proxy localhost:8080
}

# 서비스 관리
sudo systemctl status caddy
sudo systemctl restart caddy
sudo journalctl -u caddy -f  # 로그 확인
```

---

## Terraform 명령어

### 초기화 및 적용

```bash
terraform init              # 초기화
terraform validate          # 구문 검증
terraform plan              # 변경사항 미리보기
terraform apply             # 적용
terraform apply -auto-approve
```

### 상태 확인

```bash
terraform state list        # 리소스 목록
terraform output            # 출력값 확인
```

### 삭제

```bash
terraform destroy                          # 전체 삭제
terraform destroy -target=module.compute   # 특정 모듈만 삭제
```

---

## 일반적인 워크플로우

```bash
# 1. 초기화
terraform init

# 2. 코드 검증
terraform validate

# 3. 변경사항 확인
terraform plan -out=changes.tfplan

# 4. 변경사항 적용
terraform apply changes.tfplan

# 5. 서버 접속
ssh -i modules/compute/dhc-app-key.pem opc@<APP_SERVER_IP>
```

---

## OCI 관련 참고

- [OCI Terraform Provider](https://registry.terraform.io/providers/oracle/oci/latest/docs)
- [OCI Free Tier](https://www.oracle.com/cloud/free/)
- [OCI CLI 설정](https://docs.oracle.com/en-us/iaas/Content/API/SDKDocs/cliinstall.htm)
