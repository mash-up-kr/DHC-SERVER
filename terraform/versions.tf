terraform {
  required_version = ">= 1.12.0"

  required_providers {
    ncloud = {
      source  = "NaverCloudPlatform/ncloud"
      version = "~> 3.3.1" # 최신 버전 확인: https://registry.terraform.io/providers/NaverCloudPlatform/ncloud/latest
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}