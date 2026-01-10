output "instance_id" {
  description = "인스턴스 ID"
  value       = oci_core_instance.main.id
}

output "instance_state" {
  description = "인스턴스 상태"
  value       = oci_core_instance.main.state
}

output "private_ip" {
  description = "사설 IP"
  value       = oci_core_instance.main.private_ip
}

output "public_ip" {
  description = "공인 IP"
  value       = oci_core_instance.main.public_ip
}

output "private_key_pem" {
  description = "생성된 SSH 개인 키 (PEM 형식)"
  value       = var.generate_ssh_key ? tls_private_key.ssh[0].private_key_pem : null
  sensitive   = true
}

output "public_key_openssh" {
  description = "생성된 SSH 공개 키 (OpenSSH 형식)"
  value       = var.generate_ssh_key ? tls_private_key.ssh[0].public_key_openssh : null
}

output "private_key_path" {
  description = "개인 키 파일 경로"
  value       = var.generate_ssh_key ? local_file.private_key[0].filename : null
}

output "ssh_connection_command" {
  description = "SSH 접속 명령어"
  value       = var.generate_ssh_key ? "ssh -i ${local_file.private_key[0].filename} opc@${oci_core_instance.main.public_ip}" : "ssh opc@${oci_core_instance.main.public_ip}"
}

output "image_ocid" {
  description = "사용된 이미지 OCID"
  value       = local.image_ocid
}
