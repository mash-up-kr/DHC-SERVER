# 금전운 테스트 API 설계

## 전체 흐름

```
[일반 진입]
메인 → 테스트(Q1 성별/이름 → Q2 생년월일/시간) → 결과 확인
  → [랭킹 그룹 만들기] → 그룹 생성(resultId 선택) → 랭킹 확인 → 초대 링크 공유

[초대 링크 진입]
초대 링크 → 그룹 정보 확인(빈 랭킹 보드) → [랭킹 참여하기]
  → 테스트(Q1 → Q2) → 결과 확인 → 그룹 가입(resultId) → 랭킹 확인
```

---

## API 목록

| # | Method | Endpoint | 설명 |
|---|--------|----------|------|
| 1 | POST | `/api/wealth-test` | 금전운 테스트 실행 |
| 2 | GET | `/api/wealth-test/results/{resultId}` | 개인 결과 조회 |
| 3 | GET | `/api/wealth-test/stats` | 누적 참여자 수 조회 |
| 4 | POST | `/api/wealth-test/groups` | 그룹 생성 |
| 5 | GET | `/api/wealth-test/groups/invite/{inviteCode}` | 초대 코드로 그룹 정보 조회 |
| 6 | POST | `/api/wealth-test/groups/{groupId}/join` | 그룹 가입 |
| 7 | GET | `/api/wealth-test/groups/{groupId}/ranking` | 그룹 랭킹 조회 (연령대 필터) |

---

## API 상세

### 1. 금전운 테스트 실행

`POST /api/wealth-test`

DB에 저장된 500개 금전운 중 랜덤 1개를 뽑아 결과를 저장한다.

**Request**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| name | String | O | 사용자 이름 |
| gender | String | O | MALE / FEMALE |
| birthDate | LocalDate | O | 생년월일 (YYYY-MM-DD) |
| birthTime | LocalTime? | X | 태어난 시간 (HH:mm). "잘 모르겠어요" 체크 시 null |

```json
{
  "name": "홍길동",
  "gender": "MALE",
  "birthDate": "2000-01-01",
  "birthTime": "14:30"
}
```

**Response**

현재 구현된 `WealthFortuneResultResponse`를 `result` 필드에 그대로 포함하고, `resultId`와 `name`을 감싼다.

```json
{
  "resultId": "6651a...",
  "name": "홍길동",
  "result": {
    "id": "6651a0cdef0123456789abcd",
    "fortuneType": "마이더스의 손 형",
    "fortuneTypeDescription": "손만 대면 수익이 터지는 사업가",
    "fortuneDetail": "당신은 자본주의 시장에서...",
    "graphData": [
      { "age": 20, "amount": 55000000 },
      { "age": 30, "amount": 300000000 },
      { "age": 40, "amount": 800000000 },
      { "age": 50, "amount": 1800000000 },
      { "age": 60, "amount": 3500000000 },
      { "age": 70, "amount": 6000000000 },
      { "age": 80, "amount": 9000000000 }
    ],
    "events": [
      { "age": 31, "description": "...", "amount": 120000000, "iconUrl": "..." }
    ]
  }
}
```

---

### 2. 개인 결과 조회

`GET /api/wealth-test/results/{resultId}`

저장된 테스트 결과 조회. 결과 페이지 새로고침 / 결과 공유 URL 진입 시 재조회 용도.

**Response**: 테스트 실행(API #1)과 동일

---

### 3. 누적 참여자 수

`GET /api/wealth-test/stats`

메인 화면 "지금까지 N명이 참여했어요" 노출용.

**Response**

```json
{
  "totalParticipants": 389
}
```

---

### 4. 그룹 생성

`POST /api/wealth-test/groups`

그룹을 생성하고 초대 코드(8자리)를 발급한다. `resultId`가 있으면 본인 결과를 첫 멤버로 등록, 없으면 빈 그룹 생성.

**Request**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| groupName | String | O | 그룹 이름 (1~24자, 공백 포함) |
| resultId | String | X | 테스트 결과 ID. 있으면 첫 멤버로 등록, 없으면 빈 그룹 |

```json
{
  "groupName": "부자 모임",
  "resultId": "6651a..."
}
```

**Response**

```json
{
  "groupId": "6651b...",
  "groupName": "부자 모임",
  "inviteCode": "abc123xy",
  "memberCount": 1
}
```

---

### 5. 초대 코드로 그룹 정보 조회

`GET /api/wealth-test/groups/invite/{inviteCode}`

초대 링크로 진입 시 그룹 요약 노출용.

**Response**

```json
{
  "groupId": "6651b...",
  "groupName": "부자 모임",
  "memberCount": 5
}
```

---

### 6. 그룹 가입

`POST /api/wealth-test/groups/{groupId}/join`

기존 테스트 결과(`resultId`)로 그룹에 가입.

**Request**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| resultId | String | O | 테스트 결과 ID |

**Response**

```json
{
  "groupId": "6651b...",
  "groupName": "부자 모임",
  "memberCount": 6
}
```

**에러**

- `409 GROUP_FULL`: 그룹 정원 초과 (50명)
- `404 RESULT_NOT_FOUND`: resultId 없음
- `404 GROUP_NOT_FOUND`: groupId 없음

---

### 7. 그룹 랭킹 조회

`GET /api/wealth-test/groups/{groupId}/ranking?ageGroup={20|30|40|50|60|70|80|all}`

그룹 멤버를 금액 기준으로 내림차순 정렬해 반환. 연령대 필터에 따라 기준 금액이 달라진다.

**Query**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| ageGroup | String | X | `20`, `30`, `40`, `50`, `60`, `70`, `80`, `all` (default: `all`) |

**랭킹 기준**

- `ageGroup=all`: **peak 금액** (해당 멤버의 `graphData` 중 최댓값)
- `ageGroup=20|30|40|50|60|70|80`: 해당 연령대 `graphData.amount`

**Response**

```json
{
  "groupName": "부자 모임",
  "inviteCode": "abc123xy",
  "totalMemberCount": 3,
  "ageGroup": "all",
  "rankings": [
    {
      "rank": 1,
      "resultId": "6651a...",
      "name": "홍길동",
      "amount": 4500000000,
      "result": {
        "id": "6651a0cdef0123456789abcd",
        "fortuneType": "마이더스의 손 형",
        "fortuneTypeDescription": "손만 대면 수익이 터지는 사업가",
        "fortuneDetail": "당신은 자본주의 시장에서...",
        "graphData": [...],
        "events": [...]
      }
    }
  ]
}
```

- `rankings[].amount`: 선택된 `ageGroup` 기준 금액 (UI "총 45억원" 노출용)
- `rankings[].result`: 개인 결과 전체 (상세 펼침 시 사용)
- `rankings[].resultId`: 동명이인 구분용 (클라이언트 key)

---

## 데이터 구조 (MongoDB 3개 컬렉션)

### `wealth_fortune` — 금전운 콘텐츠 시드 (300개, 스프레드시트 기반)

| 필드 | 타입 | 설명 |
|------|------|------|
| _id | ObjectId | PK |
| fortuneType | String | 타입명 (예: "마이더스의 손 형") |
| fortuneTypeDescription | String | 한줄 설명 |
| fortuneDetail | String | 상세 본문 |
| graphData | `[{age, amount}]` | 20/30/40/50/60/70/80대 자산 (7개 포인트) |
| events | `[{age, description, amount, iconUrl}]` | 3개 이벤트 |

> 캐릭터 이미지(`fortuneTypeImageUrl`)는 FE 가 점수 기반으로 자체 매핑하므로 서버 응답에서 제외.

### `wealth_fortune_result` — 테스트 결과

| 필드 | 타입 | 설명 |
|------|------|------|
| _id | ObjectId | PK (= resultId) |
| name | String | 사용자 이름 |
| gender | String | MALE / FEMALE |
| birthDate | LocalDate | 생년월일 |
| birthTime | LocalTime? | 태어난 시간 (nullable) |
| fortuneId | ObjectId | wealth_fortune 참조 |
| fortuneSnapshot | Object | 비정규화 (fortuneType, graphData, events 등 전체 복사) |
| peakAmount | Long | `graphData` 중 최댓값 (랭킹 all 기준, 인덱스용) |
| createdAt | Instant | 생성 시각 |

### `wealth_fortune_group` — 그룹

| 필드 | 타입 | 설명 |
|------|------|------|
| _id | ObjectId | PK |
| name | String | 그룹 이름 |
| inviteCode | String | 초대 코드 (8자리, unique) |
| memberResultIds | `ObjectId[]` | 그룹 멤버 resultId (최대 50개) |
| createdAt | Instant | 생성 시각 |

---

## 비고

### 정책

- **사용자 식별**: 별도 인증/토큰 없음. resultId로만 식별.
- **금전운 랜덤 선택**: MongoDB `$sample` aggregation으로 500개 중 1개 추출.
- **초대 코드**: UUID 앞 8자리 등 짧고 URL-safe한 코드.
- **그룹 정원**: **50명** (초과 시 가입 불가, 운영 후 조정).
- **빈 그룹 유효**: `memberResultIds = []` 상태로 그룹 생성 가능. 랭킹 조회 시 빈 배열 반환.
- **동명이인**: 중복 허용. 프론트에서 `resultId`로 구분.
- **결과 공유 링크**: FE에서 `/results/{resultId}` 페이지 URL을 공유. 서버는 별도 short URL 생성 안 함.

### 랭킹 규칙

- 정렬: `amount` DESC
- 동점자: `createdAt` ASC (먼저 테스트한 사람 우선)
- 전체 탭: `peakAmount` (graphData 최댓값)
- 연령대 탭: 해당 age의 amount

### inviteCode 용도 상세

- **그룹 생성 시 발급** → FE가 `/wealth-test/invite/{inviteCode}` 페이지 URL을 조합해 공유
- **초대 링크 진입 시** → API #5로 그룹 정보 조회
- **랭킹 화면 공유 CTA** → API #7 응답의 `inviteCode`로 공유 URL 재생성
- `groupId`는 내부 식별용(MongoDB ObjectId), `inviteCode`는 외부 공유용 짧은 코드로 분리
