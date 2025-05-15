resource "ncloud_vpc" "main" {
  name            = var.vpc_name
  ipv4_cidr_block = var.vpc_cidr
}

resource "ncloud_network_acl" "main" {
  vpc_no = ncloud_vpc.main.id
  name   = "${var.vpc_name}-network-acl"
}