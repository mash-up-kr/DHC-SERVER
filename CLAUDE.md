# Claude Code 지침

## 서버 URL

- **운영 서버**: `https://dhc-2.duckdns.org`
- **Swagger UI**: `https://dhc-2.duckdns.org/swagger`

## Git 릴리즈 태그 생성 및 배포

1. main 브랜치에 먼저 푸시
2. 태그 생성 (lightweight 태그 사용, 본문 없이 버전만)
3. 태그 푸시
4. GitHub Release publish (배포 트리거)

```bash
git push origin main
git tag 1.2.7
git push origin 1.2.7
GH_HOST=github.com gh release create 1.2.7 --title "1.2.7" --notes ""
```

- 태그 형식: `X.Y.Z` (예: 1.2.7)
- 본문/메시지 불필요
- **중요**: `GH_HOST=github.com` 필수 (기본이 회사 Host로 연결됨)

## Google Workspace (gws CLI)

Google 관련 작업은 `gws` CLI로 수행. 사용 가능한 서비스:

| 서비스 | 설명 |
|--------|------|
| `drive` | 파일, 폴더, 공유 드라이브 관리 |
| `sheets` | 스프레드시트 읽기/쓰기 |
| `gmail` | 이메일 전송, 읽기, 관리 |
| `calendar` | 캘린더 및 이벤트 관리 |
| `docs` | Google Docs 읽기/쓰기 |
| `slides` | 프레젠테이션 읽기/쓰기 |
| `tasks` | 태스크 리스트 관리 |
| `people` | 연락처 및 프로필 관리 |
| `chat` | Chat 스페이스 및 메시지 |
| `forms` | Google Forms 읽기/쓰기 |
| `keep` | Google Keep 노트 관리 |
| `meet` | Google Meet 회의 관리 |

```bash
# 사용법
gws <service> <resource> <method> [flags]

# 예시
gws drive files list --params '{"pageSize": 10}'
gws sheets spreadsheets get --params '{"spreadsheetId": "..."}'
gws gmail users messages list --params '{"userId": "me"}'
gws docs documents get --params '{"documentId": "..."}'

# 스키마 조회
gws schema drive.files.list
```

## Notion CLI

이 프로젝트 전용 Notion 토큰을 인라인으로 전달하여 사용 (글로벌 토큰과 다름):

```bash
NOTION_TOKEN=$DHC_NOTION_TOKEN notion <command>
```

**주의**: `export`나 `notion auth login` 사용 금지 (글로벌 토큰이 변경됨)

상세 사용법: [docs/notion-cli.md](docs/notion-cli.md)
