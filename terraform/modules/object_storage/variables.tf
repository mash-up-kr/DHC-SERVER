variable "compartment_id" {
  description = "OCI Compartment OCID"
  type        = string
}

variable "project_name" {
  description = "프로젝트 이름"
  type        = string
}

variable "bucket_name" {
  description = "Object Storage 버킷 이름"
  type        = string
}

variable "bucket_access_type" {
  description = "버킷 접근 타입 (NoPublicAccess, ObjectRead, ObjectReadWithoutList)"
  type        = string
  default     = "NoPublicAccess"
}

variable "versioning" {
  description = "버전 관리 활성화"
  type        = string
  default     = "Disabled" # Enabled, Suspended, Disabled
}

variable "storage_tier" {
  description = "스토리지 티어 (Standard, Archive)"
  type        = string
  default     = "Standard"
}

variable "auto_tiering" {
  description = "자동 티어링 활성화"
  type        = string
  default     = "Disabled" # Disabled, InfrequentAccess
}

variable "freeform_tags" {
  description = "Freeform 태그"
  type        = map(string)
  default     = {}
}

variable "region" {
  description = "OCI Region"
  type        = string
}
