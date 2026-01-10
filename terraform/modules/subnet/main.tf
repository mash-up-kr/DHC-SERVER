# Subnet 생성
resource "oci_core_subnet" "main" {
  compartment_id             = var.compartment_id
  vcn_id                     = var.vcn_id
  cidr_block                 = var.subnet_cidr
  display_name               = var.subnet_name
  dns_label                  = var.subnet_dns_label
  route_table_id             = var.route_table_id
  security_list_ids          = var.security_list_ids
  availability_domain        = var.availability_domain # null이면 Regional Subnet
  prohibit_public_ip_on_vnic = var.prohibit_public_ip_on_vnic
}
