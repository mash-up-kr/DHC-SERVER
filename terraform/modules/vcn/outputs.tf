output "vcn_id" {
  description = "VCN ID"
  value       = oci_core_vcn.main.id
}

output "vcn_cidr" {
  description = "VCN CIDR 블록"
  value       = oci_core_vcn.main.cidr_blocks[0]
}

output "internet_gateway_id" {
  description = "Internet Gateway ID"
  value       = oci_core_internet_gateway.main.id
}

output "public_route_table_id" {
  description = "Public Route Table ID"
  value       = oci_core_route_table.public.id
}

output "default_security_list_id" {
  description = "기본 Security List ID"
  value       = oci_core_vcn.main.default_security_list_id
}
