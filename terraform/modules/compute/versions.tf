terraform {
  required_providers {
    oci = {
      source                = "oracle/oci"
      version               = "~> 5.46"
      configuration_aliases = [oci]
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.5"
    }
  }
}
