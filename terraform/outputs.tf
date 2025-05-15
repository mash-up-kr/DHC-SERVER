output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "subnet_id" {
  description = "서브넷 ID"
  value       = module.public_subnet.subnet_id
}

output "server_acg_id" {
  description = "서버 보안 그룹 ID"
  value       = module.acg.server_acg_id
}

output "server_id" {
  description = "서버 ID"
  value       = module.server.server_id
}

output "server_private_ip" {
  description = "서버 사설 IP"
  value       = module.server.private_ip
}

output "server_public_ip" {
  description = "서버 공인 IP"
  value       = module.server.public_ip
}

output "ssh_connection_details" {
  description = "SSH 접속 정보"
  value = {
    ip_address    = module.server.public_ip
    private_key   = module.server.private_key_path
    password_file = module.server.password_file_path
    user          = "root"
  }
}