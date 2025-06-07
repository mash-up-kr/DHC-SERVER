# VPC 모듈
module "vpc" {
  source = "./modules/vpc"

  vpc_name = "${var.project_name}-vpc"
  vpc_cidr = var.vpc_cidr
  zone     = var.zone

  providers = {
    ncloud = ncloud
  }
}

# Public Subnet
module "public_subnet" {
  source = "./modules/subnet"

  vpc_id         = module.vpc.vpc_id
  subnet_name    = "${var.project_name}-subnet"
  subnet_cidr    = var.public_subnet_cidr
  zone           = var.zone
  network_acl_id = module.vpc.network_acl_id
  subnet_type    = "PUBLIC"
  usage_type     = "GEN"

  providers = {
    ncloud = ncloud
  }

  # VPC 생성 후 서브넷 생성
  depends_on = [module.vpc]
}

# ACG + 네트워크 ACL
module "acg" {
  source = "./modules/acg"

  vpc_id         = module.vpc.vpc_id
  network_acl_id = module.vpc.network_acl_id
  project_name   = var.project_name

  providers = {
    ncloud = ncloud
  }

  # VPC 생성 후 보안 모듈 생성
  depends_on = [module.vpc]
}

# 서버 제품 정보 조회
module "server_info" {
  source = "./modules/server/spec"

  output_json = true
  output_path = "./modules/server/spec"

  providers = {
    ncloud = ncloud
  }
}

# 서버 모듈
module "server" {
  source = "./modules/server"

  subnet_id                 = module.public_subnet.subnet_id
  server_name               = var.project_name
  server_image_product_code = var.server_image_code
  server_product_code       = var.server_product_code
  block_storage_size        = 100
  acg_ids                   = [module.acg.server_acg_id]
  assign_public_ip          = true

  providers = {
    ncloud = ncloud
  }

  # 서브넷, 보안 모듈, 서버 정보 조회 후 서버 생성
  depends_on = [
    module.public_subnet,
    module.acg,
    module.server_info
  ]
}

# Object Storage 모듈 - Container Registry와 정적 파일 호스팅 지원
module "object_storage" {
  source = "./modules/object_storage"

  project_name              = var.project_name
  bucket_name               = "${var.project_name}-storage-${random_string.bucket_suffix.result}"
  bucket_public_read        = var.object_storage_public_read
  versioning                = var.object_storage_versioning
  enable_container_registry = true
  enable_static_hosting     = true

  # CORS와 라이프사이클은 NCP ncloud provider에서 직접 지원하지 않으므로
  # AWS CLI를 통해 별도로 설정해야 함
  cors_rules      = []
  lifecycle_rules = []

  tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
    Purpose   = "Container Registry and Static Files"
  }

  providers = {
    ncloud = ncloud
  }
}

# 버킷 이름을 고유하게 만들기 위한 랜덤 문자열
resource "random_string" "bucket_suffix" {
  length  = 8
  special = false
  upper   = false
}