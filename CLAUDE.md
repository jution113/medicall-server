# Medicall Server - Claude Code 지침

## 프로젝트 개요
응급 이송 자동화 시스템. 구급대원 앱에서 환자 정보 입력 → AI가 병원에 자동 전화 → 수용 가능 병원 추천.
- 현재 서버 미운영, GitHub public 레포

## 마스터 문서
노션에 시스템 아키텍처, DB 설계, 알고리즘 상세, API 명세 포함.
상세 메모리 파일:
- `memory/project.md` — 프로젝트 개요, 데이터 흐름
- `memory/db-schema.md` — RDB/Redis/MongoDB 스키마 설계
- `memory/algorithm.md` — 병원 우선순위 알고리즘 상세
- `memory/refactoring.md` — 리팩토링 우선순위 및 진행 상황

## 기술 스택
- Java 17, Spring Boot 3.4.3, Spring Data JPA, Spring Security
- MySQL 8.0 (AWS RDS), Redis (ElastiCache), MongoDB (DocumentDB)
- Twilio Programmable Voice, AWS S3, Kakao Mobility API
- Google OAuth2, JWT 인증, SSE 실시간 통신, Docker, GCP, Jenkins

## 패키지 구조
```
juton113.Atempo
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

## 알려진 기술 부채
- `SecurityConfig`: 인증 로직 전체 주석 처리 상태 (`anyRequest().permitAll()`)
- `HospitalRankingService.createRankings()`: 테스트용 하드코딩 병원 코드 혼재
- `NemcApiService.NemcHospitalData.from()`: 더미 데이터 생성 메서드가 프로덕션 코드에 혼재
- Redis N+1 조회: TODO 주석 존재, bulkGet으로 개선 필요

## 절대 하지 말 것
- `KtasLevelWeight` 전략 패턴을 if-else로 바꾸는 것
- `SecurityConfig` 문제를 조용히 삭제하거나 숨기는 것
- `application.yaml` 환경변수 참조 방식 변경 (현재 상태 유지)
- 요청하지 않은 기능 추가, 과도한 추상화

## 코딩 컨벤션
- 기존 코드 스타일 유지 (Lombok, Spring 패턴)
- 불필요한 주석, docstring 추가 금지
- 변경이 필요한 부분만 수정
