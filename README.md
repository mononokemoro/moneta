# pininicong-cashbook

Gradle **8.14+** + **Spring Boot 4.x** (Java **21**, toolchain) + **H2(파일)** 백엔드와, UI는 **`frontend/`의 React(Vite·TypeScript)** 로 만드는 워크스페이스입니다.

## 구성

| 경로 | 설명 |
|------|------|
| `backend/` | Gradle, Spring Boot, JPA, H2 |
| `frontend/` | **React** SPA (Vite 5, TypeScript) — 자세한 실행은 `frontend/README.md` |

## 사전 요구

- **JDK 21** 권장. 이 PC 기준 경로: **`C:\vPlus\config\jdk-21`**
  - **`backend/gradle.properties`** 에 `org.gradle.java.home` 로 Gradle 데몬이 이 JDK 를 쓰도록 설정해 두었습니다. 셸에 `JAVA_HOME` 을 안 잡아도 Gradle 빌드는 동일 JDK 를 사용합니다.
  - Cursor/VS Code 는 **`.vscode/settings.json`** 에서 Gradle·Java 확장이 같은 경로를 보도록 맞춰 두었습니다.
  - 다른 PC 에서는 위 두 파일의 경로만 본인 환경에 맞게 수정하면 됩니다.
- 빌드 스크립트는 **컴파일 toolchain 을 Java 21** 로 두었습니다. 로컬에 JDK 21 이 없으면 **Foojay 리졸버**가 자동으로 받을 수 있습니다 (`backend/settings.gradle.kts`).
- Node.js **18+** (프론트 개발·통합 JAR 빌드 시)

운영에서 실행만 할 때는 Spring Boot 4 가 요구하는 **JRE 17+** 이면 되지만, 이 레포는 소스·테스트를 **Java 21** 로 맞춰 두었습니다.

## 백엔드만 실행

```powershell
cd backend
.\gradlew.bat bootRun
```

- Health: `GET http://localhost:8080/api/health`
- H2 콘솔: `http://localhost:8080/h2-console` (설정은 `backend/src/main/resources/application.yml` 과 동일한 JDBC URL 사용)

H2 파일 DB는 `./data/` 아래에 생성됩니다(`jdbc:h2:file:./data/pininicong-cashbook`). 실행 위치(CWD)마다 DB 파일 위치가 달라지므로 배포 시 동일 폴더에서 실행하거나 `SPRING_DATASOURCE_URL` 등으로 경로를 고정하세요.

## 프론트만 개발 (핫 리로드)

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

브라우저: `http://localhost:5173` — Vite 가 `/api` 를 `http://localhost:8080` 으로 프록시합니다.

## 단일 JAR (UI + API 한 포트)

1. 프론트 빌드:

```powershell
cd frontend
npm install
npm run build
```

2. 백엔드에서 `bootJar` (`processResources` 가 `frontend/dist` 가 있으면 `classpath:/static` 에 복사):

```powershell
cd backend
.\gradlew.bat clean bootJar --no-daemon
```

3. 실행 (`backend` 폴더에서):

```powershell
cd backend
java -jar build\libs\pininicong-cashbook-0.0.1-SNAPSHOT.jar
```

- UI·API: `http://localhost:8080/` — `/api/health` 로 상태 확인

`frontend/dist` 가 없으면 API 전용 JAR 만 만들어집니다.

## API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/health` | 애플리케이션·DB 연결 상태 |
| GET | `/api/day?date=YYYY-MM-DD` | 선택일 가계부 화면 데이터 |
| POST | `/api/transactions` | 거래 1건 등록 (지출/수입/저축) |
| DELETE | `/api/transactions/{id}` | 거래 삭제 |
| PUT | `/api/budget/{yearMonth}` | 월 총예산 `{ "totalBudget": number }` |
| PUT | `/api/cash-balance` | 현금잔액 `{ "amount": number }` |
| PUT | `/api/day/{date}/sheet` | 일정·하단 메모 `{ "scheduleNote", "dayMemo" }` |

첫 실행 시 `CashbookDataInitializer` 가 샘플 예산·거래를 넣습니다(H2 가 비어 있을 때만).
