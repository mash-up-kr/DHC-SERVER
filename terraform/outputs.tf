# =============================================================================
# 네트워크 출력
# =============================================================================

output "vcn_id" {
  description = "VCN ID"
  value       = module.vcn.vcn_id
}

output "subnet_id" {
  description = "서브넷 ID"
  value       = module.public_subnet.subnet_id
}

output "security_list_id" {
  description = "Security List ID"
  value       = module.security_list.security_list_id
}

# =============================================================================
# App Server 출력
# =============================================================================

output "app_instance_id" {
  description = "App 인스턴스 ID"
  value       = module.compute_app.instance_id
}

output "app_private_ip" {
  description = "App 인스턴스 사설 IP"
  value       = module.compute_app.private_ip
}

output "app_public_ip" {
  description = "App 인스턴스 공인 IP"
  value       = module.compute_app.public_ip
}

output "app_ssh_command" {
  description = "App 서버 SSH 접속 명령어"
  value       = module.compute_app.ssh_connection_command
}

# =============================================================================
# DB Server 출력
# =============================================================================

output "db_instance_id" {
  description = "DB 인스턴스 ID"
  value       = module.compute_db.instance_id
}

output "db_private_ip" {
  description = "DB 인스턴스 사설 IP (MongoDB 연결용)"
  value       = module.compute_db.private_ip
}

output "ssh_private_key_path" {
  description = "SSH 개인 키 파일 경로"
  value       = module.compute_app.private_key_path
}

# =============================================================================
# Object Storage 출력
# =============================================================================

output "object_storage_bucket_name" {
  description = "Object Storage 버킷 이름"
  value       = module.object_storage.bucket_name
}

output "object_storage_namespace" {
  description = "Object Storage Namespace"
  value       = module.object_storage.namespace
}

output "object_storage_s3_endpoint" {
  description = "Object Storage S3 호환 엔드포인트"
  value       = module.object_storage.s3_endpoint
}

output "object_storage_bucket_url" {
  description = "Object Storage 버킷 URL"
  value       = module.object_storage.bucket_url
}

output "object_storage_versioning" {
  description = "Object Storage 버전 관리 상태"
  value       = module.object_storage.versioning_enabled
}

# =============================================================================
# 접속 정보 요약
# =============================================================================

output "connection_info" {
  description = "서버 접속 정보 요약"
  value = {
    app_server = {
      public_ip   = module.compute_app.public_ip
      private_ip  = module.compute_app.private_ip
      ssh_user    = "opc"
      ssh_command = module.compute_app.ssh_connection_command
    }
    db_server = {
      private_ip       = module.compute_db.private_ip
      ssh_user         = "opc"
      mongo_connection = "mongodb://${module.compute_db.private_ip}:27017/dhc?replicaSet=rs0"
    }
    key_file = module.compute_app.private_key_path
  }
}

output "storage_info" {
  description = "스토리지 정보 요약"
  value = {
    bucket_name  = module.object_storage.bucket_name
    namespace    = module.object_storage.namespace
    s3_endpoint  = module.object_storage.s3_endpoint
    instructions = module.object_storage.usage_instructions
  }
}
