terraform {
  # required_version: 필요한 Terraform 버전 지정
  required_version = ">= 1.12.0"

  # required_providers: 필요한 프로바이더 설정
  required_providers {
    # ncloud: NCloud 프로바이더 설정
    ncloud = {
      # source: 프로바이더 소스 위치
      source = "NaverCloudPlatform/ncloud"

      # version: 프로바이더 버전 (3.3.1 이상)
      version = "~> 3.3.1" # 최신 버전 확인: https://registry.terraform.io/providers/NaverCloudPlatform/ncloud/latest
    }

    # time 프로바이더 추가
    time = {
      source  = "hashicorp/time"
      version = "~> 0.9.0"
    }
  }
}