```text
├── environments/
│   ├── dev/
│   │   └── terraform.tfvars
│   └── prod/
        └── terraform.tfvars
├── modules/                      # 재사용 가능한 인프라 구성요소들을 모아둔 디렉토리
│   ├── acg/                      # 액세스 제어 그룹(Access Control Group) 모듈 - 네트워크 보안 규칙 정의
│   │   ├── main.tf               # ACG 리소스 정의 및 구성 로직
│   │   ├── outputs.tf            # 모듈에서 외부로 노출할 출력값 정의
│   │   ├── variables.tf          # 모듈에서 사용할 입력 변수 정의
│   │   └── versions.tf           # 모듈에 필요한 Terraform 및 공급자 버전 정의
│   ├── server/                   # 서버 인스턴스 생성 및 구성을 위한 모듈
│   │   ├── spec/                 # 서버 사양 관련 정보를 담은 디렉토리
│   │   │   ├── server_images.json # 사용 가능한 서버 이미지 목록 및 설정 (OS 이미지)
│   │   │   └── server_spec.json  # 서버 스펙 정의 (CPU, 메모리 등 하드웨어 구성)
│   │   ├── main.tf               # 서버 인스턴스 리소스 정의 및 생성 로직
│   │   ├── outputs.tf            # 서버 모듈의 출력값 정의 (IP, 인스턴스 ID 등)
│   │   ├── variables.tf          # 서버 모듈 입력 변수 정의
│   │   └── versions.tf           # 서버 모듈 버전 요구사항
│   ├── subnet/                   # 서브넷 구성을 위한 모듈
│   │   ├── main.tf               # 서브넷 리소스 정의 (IP 대역, 라우팅 테이블 등)
│   │   ├── outputs.tf            # 서브넷 모듈 출력값 (서브넷 ID 등)
│   │   ├── variables.tf          # 서브넷 모듈 입력 변수
│   │   └── versions.tf           # 서브넷 모듈 버전 요구사항
│   └── vpc/                      # 가상 프라이빗 클라우드(VPC) 구성 모듈
│       ├── main.tf               # VPC 리소스 정의 (네트워크 범위, DHCP 옵션 등)
│       ├── outputs.tf            # VPC 모듈 출력값 (VPC ID 등)
│       ├── variables.tf          # VPC 모듈 입력 변수
│       └── versions.tf           # VPC 모듈 버전 요구사항
├── .terraform.lock.hcl           # Terraform이 사용하는 공급자 버전 잠금 파일
├── .terraform.tfstate.lock.info  # 여러 사용자의 동시 수정 방지를 위한 상태 잠금 정보
├── main.tf                       # 루트 모듈의 주 설정 파일 - 모든 하위 모듈 호출 및 구성
├── outputs.tf                    # 루트 모듈의 출력값 정의
├── provider.tf                   # 클라우드 제공자 설정 (AWS, Azure, GCP 등)
├── README.md                     # 프로젝트 설명, 사용법 등 문서화
├── terraform.tfstate             # Terraform이 관리하는 현재 인프라 상태 정보
├── terraform.tfstate.backup      # 이전 상태의 백업 파일
├── variables.tf                  # 루트 모듈의 입력 변수 정의
└── versions.tf                   # 프로젝트에 필요한 Terraform 및 공급자 버전 요구사항
```

---

## 기본 명령어

### 1. `terraform init`

```bash
terraform init
```

- **역할**: 프로젝트 초기화, 필요한 공급자 플러그인 다운로드, 백엔드 구성 (새 프로젝트 시작 시 또는 모듈/공급자 변경 후 사용하면 됨)
- **공식 문서**: [Terraform init](https://developer.hashicorp.com/terraform/cli/commands/init)

### 2. `terraform validate`

```bash
terraform validate
```

- **역할**: 구성 파일의 구문 및 논리적 유효성 검사 (코드 변경 후, 적용 전 오류 확인하면 됨)
- **공식 문서**: [Terraform validate](https://developer.hashicorp.com/terraform/cli/commands/validate)

### 3. `terraform plan`

```bash
terraform plan
terraform plan -out=plan.tfplan
```

- **역할**: 변경 사항 미리보기, 어떤 리소스가 생성/수정/삭제될지 확인 (변경 사항 적용 전 검토할 때 쓰면 됨)
- **공식 문서**: [Terraform plan](https://developer.hashicorp.com/terraform/cli/commands/plan)

### 4. `terraform apply`

```bash
terraform apply
terraform apply plan.tfplan  
terraform apply -auto-approve
```

- **역할**: 계획된 변경 사항을 실제 인프라에 적용
- **공식 문서**: [Terraform apply](https://developer.hashicorp.com/terraform/cli/commands/apply)

### 5. `terraform destroy`

```bash
terraform destroy
terraform destroy -target=module.server  # 특정 리소스만 삭제
```

- **역할**: 모든 관리 중인 인프라 리소스 삭제
- **공식 문서**:  [Terraform 삭제](https://developer.hashicorp.com/terraform/cli/commands/destroy)

## 기타 명령어

### 6. `terraform fmt`

구성 파일을 표준 형식으로 자동 포맷팅

```bash
terraform terraform fmt -recursive
```

### 7. `terraform state`

상태 파일 조작 및 검사

```bash
terraform state list
terraform state show aws_instance.web
```

### 8. `terraform import`

기존 인프라 리소스를 Terraform 상태로 가져오기

```bash
terraform import aws_instance.example i-1234567890abcdef0
```

### 9. `terraform workspace`

여러 환경(dev, staging, prod) 관리

```bash
terraform workspace new dev
terraform workspace select prod
```

## 일반적인 워크플로우 예시

```bash
# 1. 초기화
terraform init

# 2. 코드 검증
terraform validate

# 3. 변경사항 확인
terraform plan -out=changes.tfplan

# 4. 변경사항 적용
terraform apply changes.tfplan

# 5. 필요 시 인프라 삭제
terraform destroy

# 5. 필요 시 서버 접속
ssh -i dhc-dev-key.pem root@123.456.789.10
```