variable "ncp_access_key" {
  description = "NCP 액세스 키"
  type        = string
  sensitive   = true
}

variable "ncp_secret_key" {
  description = "NCP 시크릿 키"
  type        = string
  sensitive   = true
}

variable "region" {
  description = "NCP 리전"
  type        = string
  default     = "KR"
}

variable "project_name" {
  description = "프로젝트 이름"
  type        = string
}

variable "environment" {
  description = "환경 (dev/prod)"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR"
  type        = string
  default     = "192.168.0.0/16"
}

variable "public_subnet_cidr" {
  description = "Public Subnet CIDR"
  type        = string
  default     = "192.168.1.0/24"
}

variable "zone" {
  description = "NCP 가용 구역"
  type        = string
  default     = "KR-1"
}

variable "server_image_code" {
  description = "서버 이미지 코드"
  type        = string
  default     = "SW.VSVR.OS.LNX64.ROCKY.0810.B050" # Rocky Linux 8.10
}

variable "server_product_code" {
  description = "서버 제품 코드"
  type        = string
  default     = "SVR.VSVR.HICPU.C002.M004.NET.SSD.B050.G002" # 2vCPU, 4GB RAM (High-CPU)
}

variable "object_storage_public_read" {
  description = "Object Storage 버킷 공개 읽기 권한"
  type        = bool
  default     = false
}

variable "object_storage_versioning" {
  description = "Object Storage 버전 관리 활성화"
  type        = bool
  default     = false
}

variable "object_storage_cors_enabled" {
  description = "Object Storage CORS 활성화"
  type        = bool
  default     = false
}

variable "object_storage_cors_origins" {
  description = "Object Storage CORS 허용 출처"
  type        = list(string)
  default     = ["*"]
}

variable "object_storage_lifecycle_enabled" {
  description = "Object Storage 라이프사이클 규칙 활성화"
  type        = bool
  default     = false
}

variable "object_storage_lifecycle_days" {
  description = "Object Storage 객체 만료 일수"
  type        = number
  default     = 90
}