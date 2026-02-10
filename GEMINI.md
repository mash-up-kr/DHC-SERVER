# Project DHC-SERVER Context

## Project Overview
DHC-SERVER is a backend application built with **Kotlin** and the **Ktor** framework. It serves as the API server for a service involving user fortunes, daily missions, and a reward system based on points. The application uses **MongoDB** as its primary database and integrates with **Gemini AI** for generating fortune content.

## Key Technologies
- **Language**: Kotlin
- **Framework**: Ktor (Server), Koin (Dependency Injection - inferred from common Ktor patterns, though `Dependencies.kt` handles manual DI or similar).
- **Database**: MongoDB (via `mongodb-driver-kotlin-coroutine`)
- **Build Tool**: Gradle (Kotlin DSL)
- **Infrastructure**: Docker, Terraform (Oracle Cloud)

## Architecture & Directory Structure
The project follows a Domain-Driven Design (DDD) inspired structure:

- **`src/main/kotlin/com/mashup/dhc/domain/`**: Core business logic.
  - **`model/`**: Data classes representing DB entities (`User`, `Mission`, `YearlyFortune`).
  - **`service/`**: Business logic implementations (`UserService`, `FortuneService`, `PointMultiplierService`).
- **`src/main/kotlin/com/mashup/dhc/routes/`**: API layer defining request/response DTOs and route handlers.
- **`src/main/kotlin/com/mashup/dhc/plugins/`**: Ktor configuration (Routing, Plugins, Dependencies).
- **`terraform/`**: Infrastructure definitions for OCI (Oracle Cloud Infrastructure).
- **`scripts/`**: Shell scripts for deployment and server setup.

## Key Concepts
- **User System**: Users have points, missions, and fortune history.
- **Reward System**: 
  - Users earn points to increase their **Reward Level**.
  - Levels range from LV1 (0pt) to LV10 (2200pt).
  - **Level 8 (Diamond)** requires **1300 points**.
- **Fortune**: 
  - Daily fortunes and Yearly fortunes (Gemini AI powered).
  - Yearly fortune requires Level 8 to unlock.

## Development & Operations
- **Build**: `./gradlew build`
- **Run Locally**: `./gradlew run` or via Docker Compose.
- **Database Access**: MongoDB runs in a Docker container (default port 27017).
  0~2
  3~6
  7~10
  덫에 걸린 날 (도망치는 쥐)
  부패한 지갑 (슬픈 쥐)
  초조한 (쥐)
  오늘은 지갑에서
  도망치는게 좋겠어요.
  슬픈 기운이 지갑까지 내려
  왔어요. 오늘은 조심하세요.
  쥐처럼 주변을 살피며
  새는 돈부터 체크해봐요.
  11~13
  14~17
  18~21
  공허한 날 (허무한 소)
  무기력한 돈 (묵묵한 소)
  먹이를 놓친 날 (아쉬운 호랑이)
  뭘해도 허무한 날이에요.
  오늘은 힘을 아껴두세요.
  묵묵히 버티는 흐름이에요.
  괜히 움직이면 피곤해져요.
  기회가 스쳐가요. 욕심을
  줄이면 아쉬움도 줄어요.
  22~24
  25~28
  29~32
  조용한 날 (조용한 호랑이)
  불안한 금전운 (겁먹은 토끼)
  조심스러운 날 (조용한 토끼)
  상황을 읽는 기운이에요.
  움직이기보단 지켜보세요.
  불안이 판단을 흐려요.
  꼭 필요한 것만 챙겨요!
  적게 움직이고, 조심하면
  손해는 피할 수 있어요.
  33~35
  36~39
  40~43
  날아오를 준비 (준비하는 용)
  기회를 보는 날 (차분한 용)
  조심해야 하는 날 (신중한 뱀)
  아직은 준비의 시간이에요.
  정리한 만큼 기회가 쌓여요.
  기회는 천천히 올라와요.
  지금은 흐름을 봐야해요.
  신중함이 필요한 날,
  필요한 지출을 적어봐요.
  44~46
  47~50
  51~54
  조심해야 하는 날 (독품은 뱀)
  달릴 준비 하는 날 (얌전한 말)
  부지런한 날 (설레는 말)
  예민함이 지출로 이어져요.
  감정과 결제를 분리해봐요.
  금전운 상승 직전이에요.
  큰 지출은 아직이에요.
  기운이 올라오는 날,
  움직인 만큼 결과가 와요.
  55~57
  58~61
  62~65
  평화로운 날 (걷는 양)
  풍성한 날 (여유로운 양)
  기대되는 날 (설레는 원숭이)
  서두를 필요 없어요.
  천천히 가도 손해는 없어요.
  마음이 느슨해질 수 있어요.
  작은 보상 정도는 괜찮아요.
  생각이 많아지는 날이에요. 큰 지출은 조금 미뤄봐요.
  66~68
  69~72
  73~76
  뜻밖의 횡재 (신난 원숭이)
  빛이 스며드는 날 (설레는 닭)
  빛나는 흐름 (행복한 닭)
  재미와 기회가 함께 와요. 흥만 조심하면 좋아요.
  판단이 또렷해지는 날, 결정할 게 있다면 좋아요.
  선택과 결과가 잘 맞아요.
  정리하기에 좋은 날이에요.
  77~80
  81~84
  85~88
  행운의 떨림 (신난 강아지)
  행운이 머무는 날 (행복한 강아지)
  최고의 날 (달리는 강아지)
  금전 흐름이 좋아요.
  기준 안에서 지출은 좋아요.
  안정적인 흐름이에요.
  익숙한 선택이 가장 편해요.
  방향만 맞다면, 지출 및
  투자가 성과로 돌아와요.
  89~92
  93~96
  97~100
  행운의 날 (신난 돼지)
  최고의 날 (배부른 돼지)
  최고 행운의 날 (황금 돼지)
  풍요로운 기운이 들어와요.
  기회가 오면 잡아도 돼요!
  배부른 하루가 되겠어요.
  여기저기서 돈이 넘쳐나요.
  금전운 최고의 날!
  손대는 것마다 돈이 붙어요.