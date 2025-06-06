output "bucket_name" {
  description = "생성된 Object Storage 버킷 이름"
  value       = ncloud_objectstorage_bucket.bucket.bucket_name
}

output "bucket_id" {
  description = "생성된 Object Storage 버킷 ID"
  value       = ncloud_objectstorage_bucket.bucket.id
}

output "bucket_creation_date" {
  description = "버킷 생성 날짜"
  value       = ncloud_objectstorage_bucket.bucket.creation_date
}

output "bucket_domain_name" {
  description = "Object Storage 버킷 도메인 이름"
  value       = "${var.bucket_name}.kr.object.ncloudstorage.com"
}

output "bucket_regional_domain_name" {
  description = "Object Storage 버킷 지역별 도메인 이름"
  value       = "${var.bucket_name}.kr.object.ncloudstorage.com"
}

output "bucket_arn" {
  description = "Object Storage 버킷 ARN (S3 호환)"
  value       = "arn:aws:s3:::${var.bucket_name}"
}

output "bucket_public_read" {
  description = "버킷 공개 읽기 권한 상태"
  value       = var.bucket_public_read
}

output "versioning_enabled" {
  description = "버전 관리 활성화 상태"
  value       = var.versioning
}

output "container_registry_endpoint" {
  description = "Container Registry 엔드포인트"
  value       = "https://${var.bucket_name}.kr.object.ncloudstorage.com/container-registry"
}

output "static_files_endpoint" {
  description = "정적 파일 호스팅 엔드포인트"
  value       = "https://${var.bucket_name}.kr.object.ncloudstorage.com/static-files"
}

output "container_registry_path" {
  description = "Container Registry 경로"
  value       = "container-registry/"
}

output "static_files_path" {
  description = "정적 파일 경로"
  value       = "static-files/"
}

output "bucket_acl_rule" {
  description = "버킷 ACL 규칙"
  value       = var.bucket_public_read ? "public-read" : "private"
}

output "usage_instructions" {
  description = "Object Storage 사용 방법"
  value = {
    container_registry = {
      description      = "Container Registry로 사용하기"
      example_upload   = "aws s3 cp myapp.tar s3://${ncloud_objectstorage_bucket.bucket.bucket_name}/container-registry/myapp/latest.tar --endpoint-url https://kr.object.ncloudstorage.com"
      example_download = "aws s3 cp s3://${ncloud_objectstorage_bucket.bucket.bucket_name}/container-registry/myapp/latest.tar myapp.tar --endpoint-url https://kr.object.ncloudstorage.com"
    }
    static_files = {
      description    = "정적 파일 호스팅으로 사용하기"
      example_upload = "aws s3 cp index.html s3://${ncloud_objectstorage_bucket.bucket.bucket_name}/static-files/index.html --endpoint-url https://kr.object.ncloudstorage.com"
      public_url     = "https://${ncloud_objectstorage_bucket.bucket.bucket_name}.kr.object.ncloudstorage.com/static-files/index.html"
      note           = var.bucket_public_read ? "버킷이 public-read로 설정되어 있어 모든 파일에 공개 접근 가능" : "개별 파일에 public-read ACL 설정 필요"
    }
  }
}
