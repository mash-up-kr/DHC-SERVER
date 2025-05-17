variable "subnet_id" {
  description = "서버가 생성될 서브넷 ID"

  # type: 변수 타입 지정 (문자열)
  type = string
}

variable "server_name" {
  description = "서버의 이름"

  # type: 변수 타입 지정 (문자열)
  type = string
}

variable "server_image_product_code" {
  description = "서버 이미지 제품 코드"

  # type: 변수 타입 지정 (문자열)
  type = string
}

variable "server_product_code" {
  description = "서버 제품 코드"

  # type: 변수 타입 지정 (문자열)
  type = string
}

variable "block_storage_size" {
  description = "블록 스토리지 크기 (GB)"

  # type: 변수 타입 지정 (숫자)
  type = number

  # default: 기본값 50GB
  default = 50
}

variable "acg_ids" {
  description = "적용할 보안 그룹(ACG) ID 목록"

  # type: 변수 타입 지정 (문자열 리스트)
  type = list(string)

  # default: 기본값은 빈 리스트
  default = []
}

variable "init_script_no" {
  description = "초기화 스크립트 번호"

  # type: 변수 타입 지정 (문자열)
  type = string

  # default: 기본값은 빈 문자열
  default = ""
}

variable "assign_public_ip" {
  description = "공인 IP 할당 여부"

  # type: 변수 타입 지정 (불리언)
  type = bool

  # default: 기본값 false (할당하지 않음)
  default = false
}