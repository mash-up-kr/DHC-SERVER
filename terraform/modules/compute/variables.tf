variable "compartment_id" {
  description = "OCI Compartment OCID"
  type        = string
}

variable "availability_domain" {
  description = "Availability Domain"
  type        = string
}

variable "subnet_id" {
  description = "서브넷 ID"
  type        = string
}

variable "instance_name" {
  description = "인스턴스 이름"
  type        = string
}

variable "instance_shape" {
  description = "인스턴스 Shape"
  type        = string
  default     = "VM.Standard.E4.Flex"
}

variable "instance_ocpus" {
  description = "OCPU 수"
  type        = number
  default     = 2
}

variable "instance_memory_in_gbs" {
  description = "메모리 크기 (GB)"
  type        = number
  default     = 4
}

variable "boot_volume_size_in_gbs" {
  description = "부트 볼륨 크기 (GB)"
  type        = number
  default     = 100
}

variable "image_ocid" {
  description = "이미지 OCID (빈 값이면 최신 Oracle Linux 8 사용)"
  type        = string
  default     = ""
}

variable "ssh_public_key" {
  description = "SSH 공개 키"
  type        = string
  default     = ""
}

variable "ssh_public_key_path" {
  description = "SSH 공개 키 파일 경로"
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}

variable "assign_public_ip" {
  description = "Public IP 할당 여부"
  type        = bool
  default     = true
}

variable "generate_ssh_key" {
  description = "SSH 키 자동 생성 여부"
  type        = bool
  default     = true
}
