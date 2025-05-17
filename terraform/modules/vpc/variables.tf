variable "vpc_name" {
  description = "VPC 이름"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
}

variable "zone" {
  description = "가용 영역"
  type        = string
  default     = "KR-1"
}