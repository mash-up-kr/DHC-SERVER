# NCP Object Storage 버킷 생성
resource "ncloud_objectstorage_bucket" "bucket" {
  bucket_name = var.bucket_name
}

# 버킷 ACL 설정
resource "ncloud_objectstorage_bucket_acl" "bucket_acl" {
  bucket_name = ncloud_objectstorage_bucket.bucket.bucket_name
  rule        = var.bucket_public_read ? "public-read" : "private"
}