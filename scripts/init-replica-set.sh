#!/bin/bash

echo "MongoDB Replica Set 초기화 시작..."

max_retries=30
retry_count=0

while [ $retry_count -lt $max_retries ]; do
    if docker exec mongodb mongosh --eval "db.adminCommand('ping')" &>/dev/null; then
        echo "MongoDB 헬스체크 성공"
        break
    fi
    retry_count=$((retry_count + 1))
    echo "MongoDB 대기 중... ($retry_count/$max_retries)"
    sleep 2
done

if [ $retry_count -eq $max_retries ]; then
    echo "MongoDB 시작 실패"
    exit 1
fi

echo "Replica Set 초기화 중..."
docker exec mongodb mongosh --eval "
try {
    rs.status();
    print('Replica Set이 이미 초기화되어 있습니다.');
} catch (e) {
    print('Replica Set 초기화 중...');
    rs.initiate({
        _id: 'rs0',
        members: [{
            _id: 0,
            host: 'localhost:27017'
        }]
    });
    print('Replica Set 초기화 완료');
}
"

sleep 5

echo "최종 Replica Set 상태:"
docker exec mongodb mongosh --eval "rs.status().members.forEach(m => print(m.name + ': ' + m.stateStr))"

echo "MongoDB Replica Set 초기화 완료"