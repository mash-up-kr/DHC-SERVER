terraform {
  required_providers {
    ncloud = {
      source                = "NaverCloudPlatform/ncloud"
      version               = "~> 3.3.1"
      configuration_aliases = [ncloud]
    }
  }
}