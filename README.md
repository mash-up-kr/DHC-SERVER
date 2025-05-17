# 돔황챠,,,🚗

## API 관련 문의는 맨 앞사람에게

|                         조성아                         |                        이준표                         |                       김도현                       |
|:---------------------------------------------------:|:--------------------------------------------------:|:-----------------------------------------------:|
| ![조성아](https://github.com/seongahjo.png?size=100) | ![이준표](https://github.com/wnsvy607.png?size=100) | ![김도현](https://github.com/k-diger.png?size=100) |
|   [*seongahjo*](https://github.com/seongahjo)   |   [*wnsvy607*](https://github.com/wnsvy607)    |     [*k-diger*](https://github.com/k-diger)     |

---

## 이모저모 돔황챠 서버의 컨셉

### 1. 몽고답게 쓰기

- 도메인 객체 파일 최소화
    - 애그리게이트 루트를 끌고오는거처럼 다때려박기 == 사용자가 루트임

### 2. 코틀린답게 쓰기

- 확장 함수는 확실하게 동사느낌나면 해보기
    - 알잘딱으로!
- Infix
- DSL(일종의 람다)
- 한 파일에 다넣기 (샘플 참고)

### 3. DTO

- 도메인 단위로 뿌리기 (이건 API 어케 조립할지 물어보는 비용 있을 수 있음)
- 화면 단위로 뿌리기 (굳이 커뮤니케이션 더 안해도 알아서 쓸 수 있음) --> 이걸로 갈게유

### 4. 테스트 프레임워크

- JUnit
- Kotest

### 5. R&R

-

### 6. ETC

- Ktlint

---

## 현재 패키지 구조 및 설명

```
com.mashup.dhc
├── Application.kt   # main entry point
├── plugins          # Ktor 플러그인 설정
├── routes           # 라우팅 핸들러
├── domain           # 도메인 모델
│   ├── model        # 도메인 모델 클래스
│   └── service      # 도메인 서비스
└── utils            # 공통 유틸리티
```

---

## 테스트

- [Kotest](https://kotest.io/)를 사용하여 테스트를 작성할 수 있습니다.
- 테스트 실행:  
  ```
  ./gradlew test
  ```

`
````

