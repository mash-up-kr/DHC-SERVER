variable "compartment_id" {
  description = "OCI Compartment OCID"
  type        = string
}

variable "vcn_name" {
  description = "VCN 이름"
  type        = string
}

variable "vcn_cidr" {
  description = "VCN CIDR 블록"
  type        = string
}

variable "vcn_dns_label" {
  description = "VCN DNS 레이블"
  type        = string
  default     = "vcn"
}
