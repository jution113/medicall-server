# Medicall Server - Claude Code 지침

## 프로젝트 개요
응급 이송 자동화 시스템. 구급대원 앱에서 환자 정보 입력 → AI가 병원에 자동 전화 → 수용 가능 병원 추천.
- 현재 서버 미운영, GitHub public 레포

## 마스터 문서
노션에 시스템 아키텍처, DB 설계, 알고리즘 상세, API 명세 포함 (링크는 `CLAUDE.local.md` 참고)

## 기술 스택
- Java 17, Spring Boot 3.4.3, Spring Data JPA, Spring Security
- MySQL 8.0 (AWS RDS), Redis (ElastiCache), MongoDB (DocumentDB)
- Twilio Programmable Voice, AWS S3, Kakao Mobility API
- Google OAuth2, JWT 인증, SSE 실시간 통신, Docker, GCP, Jenkins

## 패키지 구조
```
juton113.Medicall
├── domain/
│   ├── hospital/          # 병원 기본 정보 (월 1회 서울시 API 동기화)
│   ├── hospitalRanking/   # KTAS 연동 우선순위 알고리즘 결과
│   ├── incident/          # 응급 상황 (최상위 마스터 레코드)
│   ├── searchSession/     # 탐색 세션 (재탐색 이력)
│   ├── inquiryLog/        # 통화 문의 기록 (Twilio 연동)
│   ├── callArtifact/      # 통화 결과물 (MongoDB)
│   ├── member/            # 구급대원/상황실 사용자
│   └── reviewLog/         # 상황실 검토 로그 (다형적 관계)
└── global/
    ├── auth/              # JWT, OAuth2, Spring Security
    ├── config/            # Twilio, S3, Redis, CORS 등 설정
    ├── external/          # KakaoMobility, NemcApi 외부 API 클라이언트
    └── util/              # Redis 유틸, 공통 에러 코드
```

## 핵심 설계 원칙
1. **KTAS 연동 동적 알고리즘**: 환자 중증도에 따라 가중치 동적 변경
2. **설명 가능한 AI (XAI)**: score_details JSON에 각 요소별 점수 근거 저장
3. **규칙 기반 채점**: symptom-rules.yaml 기반, LLM 미사용 (신뢰성·속도)
4. **전화 자동화**: Twilio로 병원에 자동 전화 (시스템 거절 회피 현실 수용)

## 반드시 보존할 코드

### KtasLevelWeight — 절대 단순화 금지
`domain/hospitalRanking/enums/KtasLevelWeight.java`
- enum + 추상 메서드 오버라이드로 KTAS 등급별 가중치 전략 캡슐화 (전략 패턴)
- 설계 의도: 등급별로 판단 기준 자체가 달라진다는 것을 다형성으로 표현
- **if-else로 바꾸지 말 것**

### Redis 거절 쿨다운 로직
`global/util/RedisService.java`, `domain/hospitalRanking/HospitalRankingService.java`
- 거절 사유 카테고리화 + TTL 기반 가중치 조정 — 로직 유지, 가독성만 개선 대상

## 알려진 기술 부채 (우선순위 순)
- 로그/모니터링: 파일 로깅만 존재, Spring Boot Actuator 및 구조화 로깅 미적용
- docker-compose: Dockerfile은 존재하나 로컬 개발 환경용 docker-compose.yml 없음
- `NemcApiService.NemcHospitalData.from()`: 더미 데이터 생성 메서드. 현재는 `HospitalRankingServiceQueryCountTest`의 테스트 픽스처 생성용으로만 호출됨 — 프로덕션 코드가 아닌 테스트 코드로 이동 대상

## 절대 하지 말 것
- `KtasLevelWeight` 전략 패턴을 if-else로 바꾸는 것
- `application.yaml` 환경변수 참조 방식 변경 (현재 상태 유지)
- 요청하지 않은 기능 추가, 과도한 추상화

## 코딩 컨벤션
- 기존 코드 스타일 유지 (Lombok, Spring 패턴)
- 불필요한 주석, docstring 추가 금지
- 변경이 필요한 부분만 수정

## 커밋 및 PR 메시지 컨벤션
squash merge 사용 — PR 제목이 그대로 최종 커밋 메시지가 됨. 따라서 브랜치명/커밋 메시지/PR 제목에 동일한 타입 태그를 일관되게 사용.

### 타입 태그
- `feat`: 실제 사용자·운영 동작에 영향을 주는 새 기능
- `fix`: 버그 수정
- `refactor`: 동작 변화 없는 구조 개선
- `chore`: 운영 코드 경로에 영향 없는 개발/테스트 인프라, 설정, 잡무성 작업 (예: `@Profile("local")`로 갇힌 로컬 전용 도구)
- `test`: 테스트 코드만 추가/수정
- `docs`: 문서만 변경

feat과 chore의 구분 기준은 "운영 사용자에게 실질적 영향을 주는가"

### 브랜치명
`type/kebab-case-설명` (예: `feat/web-login-token-header-response`, `chore/local-jwt-test-login`)

### 커밋 메시지
- 제목: `type: 한글 설명` — 설명형 문장, 끝에 마침표 없음
- 본문(필요시): "무엇을 바꿨는지"가 아니라 "왜 필요했는지"(배경·문제 상황)를 1~3문장으로. diff로 알 수 있는 내용은 반복하지 않음
- 하나의 커밋은 하나의 논리적 단위로 분리 (예: 인프라 수정과 기능 추가는 별도 커밋)

### PR 제목
커밋 메시지와 동일한 `type: 한글 설명` 형식

### PR 본문 템플릿
```markdown
## 배경
(왜 이 작업이 필요했는지)

## 변경 사항
- (무엇을 바꿨는지, 파일/컴포넌트 단위로)

## 제한 사항
(있다면: 알려진 한계, TODO, 후속 작업)

## 테스트
- [ ] (수동/자동 확인 체크리스트)
```
