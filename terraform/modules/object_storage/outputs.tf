output "bucket_name" {
  description = "생성된 Object Storage 버킷 이름"
  value       = oci_objectstorage_bucket.bucket.name
}

output "bucket_id" {
  description = "생성된 Object Storage 버킷 ID"
  value       = oci_objectstorage_bucket.bucket.bucket_id
}

output "namespace" {
  description = "Object Storage Namespace"
  value       = data.oci_objectstorage_namespace.ns.namespace
}

output "bucket_access_type" {
  description = "버킷 접근 타입"
  value       = oci_objectstorage_bucket.bucket.access_type
}

output "versioning_enabled" {
  description = "버전 관리 활성화 상태"
  value       = oci_objectstorage_bucket.bucket.versioning
}

output "storage_tier" {
  description = "스토리지 티어"
  value       = oci_objectstorage_bucket.bucket.storage_tier
}

# S3 호환 엔드포인트
output "s3_endpoint" {
  description = "S3 호환 API 엔드포인트"
  value       = "https://${data.oci_objectstorage_namespace.ns.namespace}.compat.objectstorage.${var.region}.oraclecloud.com"
}

output "bucket_url" {
  description = "Object Storage 버킷 URL"
  value       = "https://objectstorage.${var.region}.oraclecloud.com/n/${data.oci_objectstorage_namespace.ns.namespace}/b/${var.bucket_name}/o/"
}

output "usage_instructions" {
  description = "Object Storage 사용 방법"
  value = {
    s3_compatible = {
      description = "S3 호환 API 사용 방법"
      endpoint    = "https://${data.oci_objectstorage_namespace.ns.namespace}.compat.objectstorage.${var.region}.oraclecloud.com"
      example     = "aws s3 ls s3://${var.bucket_name} --endpoint-url https://${data.oci_objectstorage_namespace.ns.namespace}.compat.objectstorage.${var.region}.oraclecloud.com"
    }
    oci_cli = {
      description = "OCI CLI 사용 방법"
      example     = "oci os object list -bn ${var.bucket_name}"
    }
  }
}
