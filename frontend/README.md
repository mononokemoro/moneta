# Cashbook — React UI

이 폴더는 **React 18** + **TypeScript** + **Vite 5** 로 만든 단일 페이지(SPA) 프론트엔드입니다.

## 스크립트

| 명령 | 설명 |
|------|------|
| `npm install` | 의존성 설치 |
| `npm run dev` | 개발 서버 `http://localhost:5173` (핫 리로드) |
| `npm run build` | `dist/` 프로덕션 빌드 → 백엔드 JAR에 포함 가능 |
| `npm run preview` | 빌드 결과 미리보기 |

## 백엔드와 함께 쓰기

1. API: `backend`에서 `.\gradlew.bat bootRun` → `http://localhost:8080`
2. UI: 여기서 `npm run dev` → `http://localhost:5173`

`vite.config.ts` 에서 `/api` 요청을 `http://localhost:8080` 으로 **프록시**하므로, 브라우저에서는 `fetch("/api/health")` 처럼 상대 경로로 호출하면 됩니다.

## 소스 구조 (요약)

```
src/
  main.tsx    # React 루트 마운트
  App.tsx     # 메인 화면
  api.ts      # API 래퍼
  index.css / App.css
```

라우팅·상태관리 라이브러리는 필요할 때 `npm install` 로 추가하면 됩니다.
