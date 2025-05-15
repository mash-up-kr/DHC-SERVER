variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "network_acl_id" {
  description = "네트워크 ACL ID"
  type        = string
}

variable "project_name" {
  description = "프로젝트 이름"
  type        = string
}

variable "environment" {
  description = "환경 (개발/스테이징/프로덕션 등)"
  type        = string
}