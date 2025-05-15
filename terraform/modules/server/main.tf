# 네트워크 인터페이스 생성
resource "ncloud_network_interface" "main" {
  subnet_no             = var.subnet_id
  name                  = "${var.server_name}-nic"
  access_control_groups = var.acg_ids
}

# SSH 로그인 키 생성
resource "ncloud_login_key" "server_key" {
  key_name = "${var.server_name}-key-${formatdate("YYMMDDHHmm", timestamp())}"
}

# 생성된 개인 키를 로컬 파일로 저장
resource "local_file" "private_key" {
  content         = ncloud_login_key.server_key.private_key
  filename        = "${path.module}/${var.server_name}-key.pem"
  file_permission = "0600" # 보안을 위한 적절한 권한 설정
}

# 서버 생성
resource "ncloud_server" "main" {
  subnet_no                 = var.subnet_id
  name                      = var.server_name
  server_image_product_code = var.server_image_product_code
  server_product_code       = var.server_product_code
  login_key_name            = ncloud_login_key.server_key.key_name

  # 네트워크 인터페이스 설정
  network_interface {
    network_interface_no = ncloud_network_interface.main.id
    order                = 0
  }
}

# 서버 생성 후 루트 비밀번호 조회
data "ncloud_root_password" "server_password" {
  server_instance_no = ncloud_server.main.id
  private_key        = ncloud_login_key.server_key.private_key

  # 서버가 완전히 생성된 후에 실행하기 위한 의존성 설정
  depends_on = [ncloud_server.main]
}

# 비밀번호를 로컬 파일로 저장
resource "local_file" "root_password" {
  content         = data.ncloud_root_password.server_password.root_password
  filename        = "${path.module}/${var.server_name}-password.key"
  file_permission = "0600" # 보안을 위한 적절한 권한 설정
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