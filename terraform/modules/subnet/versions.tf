terraform {
  required_providers {
    oci = {
      source                = "oracle/oci"
      version               = "~> 5.46"
      configuration_aliases = [oci]
    }
  }
}
