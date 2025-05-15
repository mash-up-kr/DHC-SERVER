output "server_id" {
  description = "서버 ID"
  value       = ncloud_server.main.id
}

output "server_no" {
  description = "서버 인스턴스 번호"
  value       = ncloud_server.main.instance_no
}

output "private_ip" {
  description = "사설 IP"
  value       = ncloud_server.main.private_ip
}

output "public_ip" {
  description = "서버의 공인 IP"
  value       = try(ncloud_public_ip.server[0].public_ip, null)
}

output "login_key_name" {
  description = "서버 로그인 키 이름"
  value       = ncloud_login_key.server_key.key_name
}

output "private_key_path" {
  description = "개인 키(.pem) 파일 경로"
  value       = local_file.private_key.filename
}

output "root_password" {
  description = "서버 루트 계정 비밀번호"
  value       = data.ncloud_root_password.server_password.root_password
  sensitive   = true # 비밀번호를 민감한 정보로 표시
}

output "password_file_path" {
  description = "루트 비밀번호 파일 경로"
  value       = local_file.root_password.filename
}