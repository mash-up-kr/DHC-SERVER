# VPC 모듈
module "vpc" {
  source = "./modules/vpc"

  vpc_name = "${var.project_name}-${var.environment}-vpc"
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
  subnet_name    = "${var.project_name}-${var.environment}-subnet"
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
  environment    = var.environment

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
  server_name               = "${var.project_name}-${var.environment}"
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