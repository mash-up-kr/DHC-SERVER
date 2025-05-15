resource "ncloud_subnet" "main" {
  # vpc_no: 서브넷이 속할 VPC ID를 var.vpc_id 변수에서 가져옴
  vpc_no = var.vpc_id

  # subnet: 서브넷의 CIDR 블록을 var.subnet_cidr 변수에서 가져옴
  subnet = var.subnet_cidr

  # zone: 서브넷이 생성될 가용 구역을 var.zone 변수에서 가져옴
  zone = var.zone

  # network_acl_no: 적용할 네트워크 ACL ID를 var.network_acl_id 변수에서 가져옴
  network_acl_no = var.network_acl_id

  # subnet_type: 서브넷 타입(PUBLIC/PRIVATE)을 var.subnet_type 변수에서 가져옴
  subnet_type = var.subnet_type

  # name: 서브넷의 이름을 var.subnet_name 변수에서 가져옴
  name = var.subnet_name

  # usage_type: 서브넷 사용 유형(GEN/LOADB/BM)을 var.usage_type 변수에서 가져옴
  usage_type = var.usage_type
}