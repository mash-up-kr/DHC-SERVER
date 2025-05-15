output "server_images" {
  description = "모든 서버 이미지 목록"
  value       = data.ncloud_server_images.server_images.server_images
}

output "server_specs" {
  description = "모든 서버 스펙 목록"
  value       = data.ncloud_server_specs.server_specs.server_spec_list
}

output "rockylinux_products" {
  description = "Rocky Linux 호환 서버 제품 목록"
  value       = data.ncloud_server_products.server_products.server_products
}