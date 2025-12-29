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
# Compute 출력
# =============================================================================

output "instance_id" {
  description = "인스턴스 ID"
  value       = module.compute.instance_id
}

output "instance_private_ip" {
  description = "인스턴스 사설 IP"
  value       = module.compute.private_ip
}

output "instance_public_ip" {
  description = "인스턴스 공인 IP"
  value       = module.compute.public_ip
}

output "ssh_connection_command" {
  description = "SSH 접속 명령어"
  value       = module.compute.ssh_connection_command
}

output "ssh_private_key_path" {
  description = "SSH 개인 키 파일 경로"
  value       = module.compute.private_key_path
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
    public_ip   = module.compute.public_ip
    private_ip  = module.compute.private_ip
    ssh_user    = "opc" # Oracle Linux 기본 유저
    ssh_command = module.compute.ssh_connection_command
    key_file    = module.compute.private_key_path
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
