# 서버 이미지 목록 조회
data "ncloud_server_images" "server_images" {
}

# 서버 스펙 목록 조회
data "ncloud_server_specs" "server_specs" {
}

data "ncloud_server_products" "server_products" {
  // Search by 'Rocky 8.10 (64-bit)' image vpc
  server_image_product_code = "SW.VSVR.OS.LNX64.ROCKY.0810.B050"
}

# 매번 생성을 위한 null 리소스
resource "null_resource" "timestamp_trigger" {
  # 매번 새로운 값으로 변경되도록 설정
  triggers = {
    timestamp = timestamp()
  }
}

# 모든 서버 이미지 목록 JSON 파일 생성
resource "local_file" "all_server_images_json" {
  count = var.output_json ? 1 : 0

  filename        = "${var.output_path}/all_server_images.json"
  content         = jsonencode(data.ncloud_server_images.server_images.server_images)
  file_permission = "0644"

  # null_resource의 트리거로 갱신 유도
  depends_on = [null_resource.timestamp_trigger]
}

# 모든 서버 스펙 목록 JSON 파일 생성
resource "local_file" "all_server_specs_json" {
  count = var.output_json ? 1 : 0

  filename        = "${var.output_path}/all_server_specs.json"
  content         = jsonencode(data.ncloud_server_specs.server_specs.server_spec_list)
  file_permission = "0644"

  # null_resource의 트리거로 갱신 유도
  depends_on = [null_resource.timestamp_trigger]
}

# RockyLinux 상품 목록 JSON 파일 생성
resource "local_file" "all_rockylinux_products_json" {
  count = var.output_json ? 1 : 0

  filename        = "${var.output_path}/all_rockylinux_products.json"
  content         = jsonencode(data.ncloud_server_products.server_products)
  file_permission = "0644"

  # null_resource의 트리거로 갱신 유도
  depends_on = [null_resource.timestamp_trigger]
}