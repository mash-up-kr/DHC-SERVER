variable "output_json" {
  description = "JSON 파일 출력 여부"
  type        = bool
  default     = true
}

variable "output_path" {
  description = "JSON 파일 출력 경로"
  type        = string
  default     = "."
}