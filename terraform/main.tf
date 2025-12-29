# =============================================================================
# Data Sources
# =============================================================================

# Availability Domains 조회
data "oci_identity_availability_domains" "ads" {
  compartment_id = var.tenancy_ocid
}

# =============================================================================
# VCN 모듈
# =============================================================================

module "vcn" {
  source = "./modules/vcn"

  compartment_id = var.compartment_id
  vcn_name       = "${var.project_name}-vcn"
  vcn_cidr       = var.vcn_cidr
  vcn_dns_label  = replace(lower(var.project_name), "-", "")

  providers = {
    oci = oci
  }
}

# =============================================================================
# Security List 모듈
# =============================================================================

module "security_list" {
  source = "./modules/security_list"

  compartment_id = var.compartment_id
  vcn_id         = module.vcn.vcn_id
  project_name   = var.project_name

  providers = {
    oci = oci
  }

  depends_on = [module.vcn]
}

# =============================================================================
# Public Subnet 모듈
# =============================================================================

module "public_subnet" {
  source = "./modules/subnet"

  compartment_id             = var.compartment_id
  vcn_id                     = module.vcn.vcn_id
  subnet_name                = "${var.project_name}-public-subnet"
  subnet_cidr                = var.public_subnet_cidr
  subnet_dns_label           = "pubsubnet"
  route_table_id             = module.vcn.public_route_table_id
  security_list_ids          = [module.security_list.security_list_id]
  prohibit_public_ip_on_vnic = false # Public Subnet

  providers = {
    oci = oci
  }

  depends_on = [module.vcn, module.security_list]
}

# =============================================================================
# Compute Instance 모듈
# =============================================================================

module "compute" {
  source = "./modules/compute"

  compartment_id          = var.compartment_id
  availability_domain     = data.oci_identity_availability_domains.ads.availability_domains[0].name
  subnet_id               = module.public_subnet.subnet_id
  instance_name           = var.project_name
  instance_shape          = var.instance_shape
  instance_ocpus          = var.instance_ocpus
  instance_memory_in_gbs  = var.instance_memory_in_gbs
  boot_volume_size_in_gbs = var.boot_volume_size_in_gbs
  image_ocid              = var.instance_image_ocid
  ssh_public_key          = var.ssh_public_key
  ssh_public_key_path     = var.ssh_public_key_path
  assign_public_ip        = true
  generate_ssh_key        = true # SSH 키 자동 생성

  providers = {
    oci = oci
  }

  depends_on = [module.public_subnet]
}

# =============================================================================
# Object Storage 모듈
# =============================================================================

module "object_storage" {
  source = "./modules/object_storage"

  compartment_id     = var.compartment_id
  project_name       = var.project_name
  bucket_name        = "${var.project_name}-storage"
  bucket_access_type = var.object_storage_public_read ? "ObjectRead" : "NoPublicAccess"
  versioning         = var.object_storage_versioning ? "Enabled" : "Disabled"
  storage_tier       = "Standard"

  freeform_tags = {
    Project = var.project_name
    Purpose = "Application Storage"
  }

  providers = {
    oci = oci
  }
}
