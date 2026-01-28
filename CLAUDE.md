# Claude Code 지침

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
