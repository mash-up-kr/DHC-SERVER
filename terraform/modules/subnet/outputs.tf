output "subnet_id" {
  description = "서브넷 ID"
  value       = oci_core_subnet.main.id
}

output "subnet_cidr" {
  description = "서브넷 CIDR 블록"
  value       = oci_core_subnet.main.cidr_block
}

output "subnet_domain_name" {
  description = "서브넷 도메인 이름"
  value       = oci_core_subnet.main.subnet_domain_name
}
