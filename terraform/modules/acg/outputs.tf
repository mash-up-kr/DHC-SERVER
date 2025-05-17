output "server_acg_id" {
  description = "서버 ACG ID"
  value       = ncloud_access_control_group.server.id
}

output "network_acl_rule_id" {
  description = "네트워크 ACL 규칙 ID"
  value       = ncloud_network_acl_rule.main.id
}