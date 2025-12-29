# Object Storage Namespace 조회
data "oci_objectstorage_namespace" "ns" {
  compartment_id = var.compartment_id
}

# Object Storage 버킷 생성
resource "oci_objectstorage_bucket" "bucket" {
  compartment_id = var.compartment_id
  namespace      = data.oci_objectstorage_namespace.ns.namespace
  name           = var.bucket_name
  access_type    = var.bucket_access_type
  storage_tier   = var.storage_tier
  auto_tiering   = var.auto_tiering
  versioning     = var.versioning

  freeform_tags = merge(var.freeform_tags, {
    Project   = var.project_name
    ManagedBy = "Terraform"
  })
}
