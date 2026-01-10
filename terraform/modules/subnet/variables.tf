variable "compartment_id" {
  description = "OCI Compartment OCID"
  type        = string
}

variable "vcn_id" {
  description = "VCN ID"
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

variable "subnet_dns_label" {
  description = "서브넷 DNS 레이블"
  type        = string
  default     = "subnet"
}

variable "route_table_id" {
  description = "Route Table ID"
  type        = string
}

variable "security_list_ids" {
  description = "Security List ID 목록"
  type        = list(string)
}

variable "availability_domain" {
  description = "Availability Domain (Regional subnet인 경우 null)"
  type        = string
  default     = null
}

variable "prohibit_public_ip_on_vnic" {
  description = "Public IP 할당 금지 여부 (Private subnet인 경우 true)"
  type        = bool
  default     = false # Public Subnet
}
