# 보안 그룹(ACG) 생성
resource "ncloud_access_control_group" "server" {
  vpc_no      = var.vpc_id
  name        = "${var.project_name}-server-acg"
  description = "서버용 보안 그룹"
}

# 보안 그룹 규칙 - HTTP 접근 허용
resource "ncloud_access_control_group_rule" "server_http" {
  access_control_group_no = ncloud_access_control_group.server.id

  inbound {
    protocol    = "TCP"
    port_range  = "80"
    ip_block    = "0.0.0.0/0"
    description = "HTTP 접근"
  }
}

# 보안 그룹 규칙 - HTTPS 접근 허용
resource "ncloud_access_control_group_rule" "server_https" {
  access_control_group_no = ncloud_access_control_group.server.id

  inbound {
    protocol    = "TCP"
    port_range  = "443"
    ip_block    = "0.0.0.0/0"
    description = "HTTPS 접근"
  }
}

# 보안 그룹 규칙 - HTTPS 접근 허용
resource "ncloud_access_control_group_rule" "server_https" {
  access_control_group_no = ncloud_access_control_group.server.id

  inbound {
    protocol    = "TCP"
    port_range  = "8080"
    ip_block    = "0.0.0.0/0"
    description = "서버 포트 직접 접근"
  }
}

# 보안 그룹 규칙 - SSH 접근 허용
resource "ncloud_access_control_group_rule" "server_ssh" {
  access_control_group_no = ncloud_access_control_group.server.id

  inbound {
    protocol    = "TCP"
    port_range  = "22"
    ip_block    = "0.0.0.0/0"
    description = "SSH 접근"
  }
}

# 보안 그룹 규칙 - 모든 아웃바운드 허용
resource "ncloud_access_control_group_rule" "server_outbound" {
  access_control_group_no = ncloud_access_control_group.server.id

  outbound {
    protocol    = "TCP"
    port_range  = "1-65535"
    ip_block    = "0.0.0.0/0"
    description = "모든 아웃바운드 트래픽"
  }
}

# 네트워크 ACL 규칙 - 모든 인/아웃바운드 허용
resource "ncloud_network_acl_rule" "main" {
  network_acl_no = var.network_acl_id

  inbound {
    priority    = 100
    protocol    = "TCP"
    rule_action = "ALLOW"
    ip_block    = "0.0.0.0/0"
    port_range  = "1-65535"
    description = "모든 인바운드 트래픽 허용"
  }

  outbound {
    priority    = 100
    protocol    = "TCP"
    rule_action = "ALLOW"
    ip_block    = "0.0.0.0/0"
    port_range  = "1-65535"
    description = "모든 아웃바운드 트래픽 허용"
  }

  depends_on = [var.network_acl_id]
}