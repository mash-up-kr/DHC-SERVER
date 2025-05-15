output "subnet_id" {
  description = "서브넷 ID"
  value       = ncloud_subnet.main.id
}

output "subnet_no" {
  description = "서브넷 번호"
  value       = ncloud_subnet.main.subnet_no
}