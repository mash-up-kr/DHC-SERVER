variable "project_name" {
  description = "프로젝트 이름"
  type        = string
}

variable "bucket_name" {
  description = "Object Storage 버킷 이름"
  type        = string
}

variable "bucket_public_read" {
  description = "버킷 공개 읽기 권한 설정"
  type        = bool
  default     = false
}

variable "cors_rules" {
  description = "CORS 규칙 설정"
  type = list(object({
    allowed_headers = list(string)
    allowed_methods = list(string)
    allowed_origins = list(string)
    expose_headers  = list(string)
    max_age_seconds = number
  }))
  default = []
}

variable "lifecycle_rules" {
  description = "라이프사이클 규칙 설정"
  type = list(object({
    id                                 = string
    enabled                            = bool
    expiration_days                    = number
    noncurrent_version_expiration_days = number
  }))
  default = []
}

variable "versioning" {
  description = "버전 관리 활성화"
  type        = bool
  default     = false
}

variable "tags" {
  description = "리소스 태그"
  type        = map(string)
  default     = {}
}

variable "enable_container_registry" {
  description = "Container Registry 지원 활성화"
  type        = bool
  default     = true
}

variable "enable_static_hosting" {
  description = "정적 파일 호스팅 활성화"
  type        = bool
  default     = true
}