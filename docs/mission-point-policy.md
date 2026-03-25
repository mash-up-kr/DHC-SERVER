# 미션 완료 및 포인트 정책

## 미션 완료 흐름

### 1. 개별 미션 완료/취소
- **API**: `PUT /api/users/{userId}/missions/{missionId}`
- **동작**: 미션의 `finished` 상태를 토글
- **포인트**: 적립 안 됨 (정산 시점까지 대기)

### 2. 오늘의 미션 완료 (유저가 직접 호출)
- **API**: `POST /api/users/{userId}/done`
- **동작**:
  1. `settleMissions()` → 완료된 미션 기반 포인트 계산 + 배율 적용 + 적립
  2. `summaryTodayMission()` → PastRoutineHistory 저장 + 새 미션 생성
- **포인트**: 배율 적용하여 적립

### 3. 자동 롤오버 (다음날 Home 접속 시)
- **API**: `GET /view/users/{userId}/home` 내부 로직
- **조건**: `todayDailyMissionList`의 `endDate`가 오늘보다 이전이면 자동 실행
- **동작**:
  1. `settleMissions()` → 완료된 미션 기반 포인트 계산 + 배율 적용 + 적립
  2. `summaryTodayMission()` → PastRoutineHistory 저장 + 새 미션 생성
- **포인트**: 배율 적용하여 적립 (/done과 동일)

## summaryTodayMission() vs settleMissions()

| | `summaryTodayMission()` | `settleMissions()` |
|---|---|---|
| 역할 | 미션 정산 (히스토리 저장 + 새 미션 생성) | 포인트 계산 + 배율 적용 + 적립 |
| 포인트 | 다루지 않음 | 계산하고 적립 |
| 호출 순서 | settleMissions() 이후 | summaryTodayMission() 이전 |

## 포인트 배율 정책

### 배율 조건 (pastRoutineHistory 기반)

| 상황 | 배율 | 판단 기준 |
|---|---|---|
| 정상 (어제 미션 수행함) | 1x | 어제 pastRoutineHistory에 finished 미션 있음 |
| 하루 안 함 (어제 미션 전부 미수행) | 2x | 어제 pastRoutineHistory에 finished 미션 없음 |
| 2일 이상 안 함 | 4x | 가장 최근 pastRoutineHistory가 2일 이상 전 |

- "안 함" = 모든 미션 (일일 + 소비습관 + 러브)을 하나도 진행하지 않음
- `lastAccessDate`는 배율 계산에 사용하지 않음 (클라이언트 메시지 표시용으로만 유지)

### 포인트 계산

미션 난이도별 기본 포인트:

| 난이도 | 포인트 |
|---|---|
| Easy (1) | 50pt |
| Medium (2) | 100pt |
| Hard (3+) | 200pt |

최종 포인트:
```
earnedPoint = (완료 미션들의 포인트 합) × 배율
```
