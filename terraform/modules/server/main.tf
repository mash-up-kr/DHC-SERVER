resource "ncloud_server" "main" {
  subnet_no                 = var.subnet_id
  name                      = var.server_name
  server_image_product_code = var.server_image_product_code
  server_product_code       = var.server_product_code

  # 네트워크 인터페이스 설정
  network_interface {
    network_interface_no = ncloud_network_interface.main.id
    order                = 0
  }

  # 초기화 스크립트 번호 (선택적)
  init_script_no = var.init_script_no != "" ? var.init_script_no : null
}

# 네트워크 인터페이스 생성
resource "ncloud_network_interface" "main" {
  subnet_no             = var.subnet_id
  name                  = "${var.server_name}-nic"
  access_control_groups = var.acg_ids
}

# 추가 볼륨 생성 (블록 스토리지)
resource "ncloud_block_storage" "main" {
  server_instance_no = ncloud_server.main.id
  name               = "${var.server_name}-storage"
  size               = var.block_storage_size
  disk_detail_type   = "SSD" # 또는 HDD
}

# Public IP 연결 리소스
resource "ncloud_public_ip" "server" {
  count = var.assign_public_ip ? 1 : 0

  server_instance_no = ncloud_server.main.id
  description        = "${var.server_name}-public-ip"
}