# 최신 Oracle Linux 8 이미지 조회
data "oci_core_images" "oracle_linux" {
  compartment_id           = var.compartment_id
  operating_system         = "Oracle Linux"
  operating_system_version = "8"
  shape                    = var.instance_shape
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"

  filter {
    name   = "display_name"
    values = ["^Oracle-Linux-8\\.\\d+-.*$"]
    regex  = true
  }
}

# SSH 키 생성 (generate_ssh_key가 true인 경우)
resource "tls_private_key" "ssh" {
  count     = var.generate_ssh_key ? 1 : 0
  algorithm = "RSA"
  rsa_bits  = 4096
}

# 생성된 개인 키를 로컬 파일로 저장
resource "local_file" "private_key" {
  count           = var.generate_ssh_key ? 1 : 0
  content         = tls_private_key.ssh[0].private_key_pem
  filename        = "${path.module}/${var.instance_name}-key.pem"
  file_permission = "0600"
}

# SSH 공개 키 결정
locals {
  ssh_public_key = var.generate_ssh_key ? tls_private_key.ssh[0].public_key_openssh : (
    var.ssh_public_key != "" ? var.ssh_public_key : file(var.ssh_public_key_path)
  )
  image_ocid = var.image_ocid != "" ? var.image_ocid : data.oci_core_images.oracle_linux.images[0].id
  # Flex Shape 여부 확인
  is_flex_shape = length(regexall("Flex$", var.instance_shape)) > 0
}

# Compute Instance 생성
resource "oci_core_instance" "main" {
  compartment_id      = var.compartment_id
  availability_domain = var.availability_domain
  display_name        = var.instance_name
  shape               = var.instance_shape

  # Flex Shape일 때만 shape_config 설정
  dynamic "shape_config" {
    for_each = local.is_flex_shape ? [1] : []
    content {
      ocpus         = var.instance_ocpus
      memory_in_gbs = var.instance_memory_in_gbs
    }
  }

  # 부트 볼륨 설정
  source_details {
    source_type             = "image"
    source_id               = local.image_ocid
    boot_volume_size_in_gbs = var.boot_volume_size_in_gbs
  }

  # 네트워크 설정
  create_vnic_details {
    subnet_id        = var.subnet_id
    display_name     = "${var.instance_name}-vnic"
    assign_public_ip = var.assign_public_ip
    hostname_label   = replace(lower(var.instance_name), "_", "-")
  }

  # SSH 키 설정
  metadata = {
    ssh_authorized_keys = local.ssh_public_key
  }

  # 에이전트 설정
  agent_config {
    is_monitoring_disabled = false
    is_management_disabled = false
  }

  # 가용성 설정 - 인스턴스 자동 복구
  availability_config {
    is_live_migration_preferred = true
    recovery_action             = "RESTORE_INSTANCE"
  }

  # 인스턴스가 삭제될 때 부트 볼륨도 함께 삭제
  preserve_boot_volume = false
}
