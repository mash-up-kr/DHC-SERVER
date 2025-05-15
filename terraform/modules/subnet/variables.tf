variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "subnet_name" {
  description = "서브넷 이름"
  type        = string
}

variable "subnet_cidr" {
  description = "서브넷 CIDR 블록"
  type        = string
}

variable "zone" {
  description = "서브넷 AZ"
  type        = string
}

variable "network_acl_id" {
  description = "네트워크 ACL ID"
  type        = string
}

variable "subnet_type" {
  description = "서브넷 타입 (PUBLIC/PRIVATE)"
  type        = string
}

variable "usage_type" {
  description = "서브넷 사용 유형 (GEN/LOADB/BM)"
  type        = string
  # default: 기본값으로 GEN(일반) 타입 사용
  default = "GEN"
}