output "server_id" {
  description = "서버 ID"

  # value: 출력할 실제 값 - 생성된 서버의 ID를 참조
  value = ncloud_server.main.id
}

output "server_no" {
  description = "서버 인스턴스 번호"

  # value: ncloud_server.main.instance_no - 서버의 인스턴스 번호를 참조
  value = ncloud_server.main.instance_no
}

output "private_ip" {
  description = "사설 IP"

  # value: ncloud_server.main.private_ip - 서버의 사설 IP 주소를 참조
  value = ncloud_server.main.private_ip
}

output "public_ip" {
  description = "서버의 공인 IP"
  # value: 공인 IP가 할당되었으면 해당 IP 반환, 없으면 null 반환
  # try 함수: 첫 번째 인자가 실패하면 두 번째 인자(null) 반환
  value = try(ncloud_public_ip.server[0].public_ip, null)
}