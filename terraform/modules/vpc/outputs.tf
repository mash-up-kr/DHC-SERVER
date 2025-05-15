output "vpc_id" {
  description = "VPC ID"
  value       = ncloud_vpc.main.id
}

output "network_acl_id" {
  description = "네트워크 ACL ID"
  value       = ncloud_network_acl.main.id
}