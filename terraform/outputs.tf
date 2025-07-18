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

output "object_storage_bucket_name" {
  description = "Object Storage 버킷 이름"
  value       = module.object_storage.bucket_name
}

output "object_storage_bucket_domain" {
  description = "Object Storage 버킷 도메인"
  value       = module.object_storage.bucket_domain_name
}

output "object_storage_bucket_arn" {
  description = "Object Storage 버킷 ARN"
  value       = module.object_storage.bucket_arn
}

output "object_storage_public_read" {
  description = "Object Storage 공개 읽기 상태"
  value       = module.object_storage.bucket_public_read
}

output "object_storage_versioning" {
  description = "Object Storage 버전 관리 상태"
  value       = module.object_storage.versioning_enabled
}

output "container_registry_endpoint" {
  description = "Container Registry 엔드포인트"
  value       = module.object_storage.container_registry_endpoint
}

output "static_files_endpoint" {
  description = "정적 파일 호스팅 엔드포인트"
  value       = module.object_storage.static_files_endpoint
}