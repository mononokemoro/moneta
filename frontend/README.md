# Cashbook — React UI

이 폴더는 **React 18** + **TypeScript** + **Vite 5** 로 만든 단일 페이지(SPA) 프론트엔드입니다.

## 실행 요약

| 목적 | 명령 | 접속 URL |
|------|------|----------|
| API만 | `backend` → `.\gradlew.bat bootRun` | http://localhost:8080 |
| 개발 (UI + API) | 백엔드 + 여기서 `npm run dev` | http://localhost:5173 |
| 배포용 단일 JAR | 프론트 `npm run build` → `backend` `bootJar` → `java -jar ...` | http://localhost:8080 |

**사전 요구:** JDK **21**, Node.js **18+**. H2 DB는 기본적으로 `%USERPROFILE%\.pininicong-cashbook\db` 에 생성됩니다. 워크스페이스 전체 설명은 루트 [`README.md`](../README.md) 를 참고하세요.

### 백엔드만

```powershell
cd backend
.\gradlew.bat bootRun
```

- Health: http://localhost:8080/api/health
- H2 콘솔: http://localhost:8080/h2-console

### 프론트 + 백엔드 (개발, 핫 리로드)

터미널 1 — 백엔드:

```powershell
cd backend
.\gradlew.bat bootRun
```

터미널 2 — 프론트:

```powershell
cd frontend
npm install
npm run dev
```

브라우저: http://localhost:5173 — Vite 가 `/api` 를 http://localhost:8080 으로 프록시합니다.

### 단일 JAR (UI + API 한 포트)

```powershell
cd frontend
npm install
npm run build

cd ..\backend
.\gradlew.bat clean bootJar --no-daemon

# backend 폴더에서 실행 (경로: backend\build\libs\...)
java -jar build\libs\pininicong-cashbook-0.0.1-SNAPSHOT.jar
```

UI·API 모두 http://localhost:8080/ (`frontend/dist` 가 없으면 API 전용 JAR 만 생성됩니다).

## 스크립트

| 명령 | 설명 |
|------|------|
| `npm install` | 의존성 설치 |
| `npm run dev` | 개발 서버 `http://localhost:5173` (핫 리로드) |
| `npm run build` | `dist/` 프로덕션 빌드 → 백엔드 JAR에 포함 가능 |
| `npm run preview` | 빌드 결과 미리보기 |

## API 프록시

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
