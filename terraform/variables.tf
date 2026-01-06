# =============================================================================
# OCI 인증 변수
# =============================================================================

variable "tenancy_ocid" {
  description = "OCI Tenancy OCID"
  type        = string
  sensitive   = true
}

variable "user_ocid" {
  description = "OCI User OCID"
  type        = string
  sensitive   = true
}

variable "fingerprint" {
  description = "OCI API Key Fingerprint"
  type        = string
  sensitive   = true
}

variable "private_key_path" {
  description = "OCI API 개인 키 파일 경로"
  type        = string
  default     = "~/.oci/oci_api_key.pem"
}

variable "region" {
  description = "OCI 리전"
  type        = string
  default     = "ap-chuncheon-1" # 춘천 리전
}

variable "compartment_id" {
  description = "OCI Compartment OCID (리소스 그룹)"
  type        = string
}

# =============================================================================
# 프로젝트 설정
# =============================================================================

variable "project_name" {
  description = "프로젝트 이름"
  type        = string
}

# =============================================================================
# 네트워크 설정
# =============================================================================

variable "vcn_cidr" {
  description = "VCN CIDR 블록"
  type        = string
  default     = "192.168.0.0/16"
}

variable "public_subnet_cidr" {
  description = "Public Subnet CIDR"
  type        = string
  default     = "192.168.1.0/24"
}

# =============================================================================
# Compute 설정
# =============================================================================

variable "instance_shape" {
  description = "Compute 인스턴스 Shape"
  type        = string
  default     = "VM.Standard.E2.1.Micro" # AMD Free Tier (1 OCPU, 1GB RAM)
}

variable "instance_ocpus" {
  description = "인스턴스 OCPU 수 (Flex Shape에서만 사용)"
  type        = number
  default     = 1
}

variable "instance_memory_in_gbs" {
  description = "인스턴스 메모리 GB (Flex Shape에서만 사용)"
  type        = number
  default     = 1
}

variable "boot_volume_size_in_gbs" {
  description = "부트 볼륨 크기 (GB) - Free Tier: 총 200GB"
  type        = number
  default     = 50
}

variable "instance_image_ocid" {
  description = "인스턴스 이미지 OCID (Oracle Linux 8)"
  type        = string
  default     = "" # 자동으로 최신 Oracle Linux 8 이미지 사용
}

variable "ssh_public_key" {
  description = "SSH 공개 키 (인스턴스 접속용)"
  type        = string
  default     = ""
}

variable "ssh_public_key_path" {
  description = "SSH 공개 키 파일 경로"
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}

# =============================================================================
# Object Storage 설정
# =============================================================================

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
