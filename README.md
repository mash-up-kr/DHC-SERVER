# DHC

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

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). You'll need
  to [request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up) to join.

## Features

Here's a list of features included in this project:

| Name                                               | Description                                                 |
|----------------------------------------------------|-------------------------------------------------------------|
| [Routing](https://start.ktor.io/p/routing-default) | Allows to define structured routes and associated handlers. |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                          | Description                                                          |
|-------------------------------|----------------------------------------------------------------------|
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `buildImage`                  | Build the docker image to use with the fat JAR                       |
| `publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

