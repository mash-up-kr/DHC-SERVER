# Security List 생성 (NCP의 ACG에 해당)
resource "oci_core_security_list" "server" {
  compartment_id = var.compartment_id
  vcn_id         = var.vcn_id
  display_name   = "${var.project_name}-security-list"

  # SSH 접근 허용 (포트 22)
  ingress_security_rules {
    protocol    = "6" # TCP
    source      = "0.0.0.0/0"
    source_type = "CIDR_BLOCK"
    description = "SSH 접근"

    tcp_options {
      min = 22
      max = 22
    }
  }

  # HTTP 접근 허용 (포트 80)
  ingress_security_rules {
    protocol    = "6" # TCP
    source      = "0.0.0.0/0"
    source_type = "CIDR_BLOCK"
    description = "HTTP 접근"

    tcp_options {
      min = 80
      max = 80
    }
  }

  # HTTPS 접근 허용 (포트 443)
  ingress_security_rules {
    protocol    = "6" # TCP
    source      = "0.0.0.0/0"
    source_type = "CIDR_BLOCK"
    description = "HTTPS 접근"

    tcp_options {
      min = 443
      max = 443
    }
  }

  # 애플리케이션 포트 접근 허용 (포트 8080)
  ingress_security_rules {
    protocol    = "6" # TCP
    source      = "0.0.0.0/0"
    source_type = "CIDR_BLOCK"
    description = "서버 포트 직접 접근"

    tcp_options {
      min = 8080
      max = 8080
    }
  }

  # ICMP (Ping) 허용 - Type 3, Code 4 (필수)
  ingress_security_rules {
    protocol    = "1" # ICMP
    source      = "0.0.0.0/0"
    source_type = "CIDR_BLOCK"
    description = "ICMP Path MTU Discovery"

    icmp_options {
      type = 3
      code = 4
    }
  }

  # 모든 아웃바운드 트래픽 허용
  egress_security_rules {
    protocol         = "all"
    destination      = "0.0.0.0/0"
    destination_type = "CIDR_BLOCK"
    description      = "모든 아웃바운드 트래픽"
  }
}
