# Medicall — 응급 이송 병원 자동 탐색 시스템

> 구급대원이 환자 정보를 입력하면, AI가 병원에 동시 전화를 걸어 수용 가능한 병원을 자동으로 찾아주는 시스템

---

## 목차
1. [문제 정의](#1-문제-정의)
2. [핵심 기능](#2-핵심-기능)
3. [시스템 아키텍처](#3-시스템-아키텍처)
4. [서비스 플로우](#4-서비스-플로우)
5. [기술 스택](#5-기술-스택)
6. [핵심 기술 구현](#6-핵심-기술-구현)
7. [데이터베이스 설계](#7-데이터베이스-설계)
8. [API 문서](#8-api-문서)
9. [관련 레포지토리](#9-관련-레포지토리)

---

## 1. 문제 정의

응급실 뺑뺑이의 근본 원인은 **병원이 시스템을 통한 거절을 회피**한다는 데 있습니다.

병원이 119 이송 요청 시스템에서 '거절'을 누르면 NEDIS(국가응급진료정보망)에 공식 기록이 남아 응급의료기관 평가에서 불이익을 받습니다. 반면 전화 통화로 거절하면 기록이 남지 않습니다. 그 결과, 병원은 시스템을 무응답으로 두고 구급대원이 수십 통의 전화를 직접 돌려야 하는 상황이 반복됩니다.

**Medicall은 병원의 행동을 바꾸려 하지 않습니다.**
병원이 선호하는 '전화'라는 방식을 그대로 존중하되, 구급대원이 수작업으로 하던 전화 걸기를 AI가 자동화합니다.

---

## 2. 핵심 기능

### KTAS 연동 동적 우선순위 알고리즘
환자의 중증도(KTAS 등급)에 따라 병원 평가 기준 자체를 동적으로 변경하는 추천 시스템입니다.

- KTAS 1 (소생): 이송 속도 90% — *"시간이 생명이다"* (가장 가까운 응급실 우선)
- KTAS 2 (긴급): 진료 역량 70% — *"역량이 생명이다"* (최종 치료 가능 병원 우선)
- KTAS 3 (응급): 속도 50% / 역량 50% — *"균형이 중요하다"*
- KTAS 4/5 (분산): 상급 병원 역가중치 — *"시스템을 보호해야 한다"*

### AI 일괄 통신 자동화
구급대원이 버튼 하나를 누르면 AI 음성봇이 선정된 병원들에 동시 전화를 걸어 수용 가능 여부와 거절 사유를 수집합니다.

### 실시간 이송 공유 대시보드
SSE(Server-Sent Events)를 통해 AI 통화 진행 상황을 구급대원 앱과 119 상황실 웹에 실시간으로 공유합니다.

---

## 3. 시스템 아키텍처

```
┌──────────────────┐     ┌──────────────────────────────────┐
│   Client Layer   │     │          Backend Layer            │
│                  │     │                                    │
│  구급대원 앱       │────▶│  Medicall-API (이 레포)            │
│  (Flutter)       │◀────│  Java / Spring Boot               │
│                  │ SSE │                                    │
│  상황실 웹         │────▶│  Medicall-AI-Gateway              │
│  (Web Dashboard) │     │  AI 음성봇 / 통화 로직              │
└──────────────────┘     └──────────────┬───────────────────┘
                                        │
              ┌─────────────────────────┼──────────────────────┐
              ▼                         ▼                       ▼
       ┌─────────────┐         ┌──────────────┐        ┌──────────────┐
       │  MySQL      │         │   Redis      │        │   MongoDB    │
       │  (AWS RDS)  │         │ (ElastiCache)│        │(DocumentDB)  │
       │  정형 데이터  │         │  세션/캐시    │        │  통화 결과물  │
       └─────────────┘         └──────────────┘        └──────────────┘
              │                                                  │
              ▼                                                  ▼
       ┌─────────────┐                                  ┌──────────────┐
       │  AWS S3     │                                  │   Twilio     │
       │  음성 녹취   │                                  │  AI 전화 통화 │
       └─────────────┘                                  └──────────────┘
```

---

## 4. 서비스 플로우

```
[구급대원 앱]
  → 환자 정보 입력 (KTAS 등급, 증상, 현재 위치)
       ↓
[Medicall-API]
  → KTAS 연동 동적 우선순위 알고리즘 실행
  → 실시간 병원 데이터 조회 (NEMC API)
  → 이송 시간 계산 (Kakao Mobility API)
  → hospital_rankings 생성 및 SSE 전송
       ↓
[구급대원] → 브리핑 패널 확인 후 '일괄 문의 시작'
       ↓
[Medicall-API → AI Gateway → Twilio]
  → 선정 병원들에 AI 음성봇 동시 전화
  → 통화 결과(수용/거절/사유)를 Webhook으로 수신
  → inquiry_logs 업데이트 + SSE로 실시간 전송
       ↓
[구급대원] → 수용 병원 확인 후 이송
```

---

## 5. 기술 스택

### Backend
![Java](https://img.shields.io/badge/Java_17-007396?style=for-the-badge&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)

### Database & Caching
![MySQL](https://img.shields.io/badge/MySQL_8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)

### Infrastructure
![AWS](https://img.shields.io/badge/AWS_(RDS/S3)-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![GCP](https://img.shields.io/badge/GCP-4285F4?style=for-the-badge&logo=googlecloud&logoColor=white)
![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)

### External APIs
![Twilio](https://img.shields.io/badge/Twilio-F22F46?style=for-the-badge&logo=twilio&logoColor=white)
- Kakao Mobility API (이송 시간 계산)
- NEMC API / 서울시 공공데이터 (실시간 병원 정보)

---

## 6. 핵심 기술 구현

### KTAS 등급별 전략 패턴 (Strategy Pattern)

각 KTAS 등급은 병원 선택 기준 자체가 다릅니다. 단순히 가중치 숫자만 다른 게 아니라, KTAS 4/5는 상급 병원 집중을 막기 위해 역가중치(100 - score)를 적용하는 등 **계산 로직 자체가 달라집니다.**

이를 enum + 추상 메서드 오버라이드로 구현하여, 각 등급의 판단 기준을 독립적인 전략으로 캡슐화했습니다.

```java
// domain/hospitalRanking/enums/KtasLevelWeight.java
public enum KtasLevelWeight {
    LEVEL_1(0.1, 0.9) {         // 이송 속도 90% 우선
        @Override
        public double calculateBaseScore(double protocolScore, double timeScore) {
            return (protocolScore * this.getProtocolWeight()) + (timeScore * this.getTimeWeight());
        }
    },
    LEVEL_4_5(0.5, 0.5) {       // 상급 병원 역가중치 적용
        @Override
        public double calculateBaseScore(double protocolScore, double timeScore) {
            double inverseProtocolScore = 100.0 - protocolScore; // 역가중치
            return (inverseProtocolScore * this.getProtocolWeight()) + (timeScore * this.getTimeWeight());
        }
    };
    // ...
    public abstract double calculateBaseScore(double protocolScore, double timeScore);
}
```

호출부에서는 등급에 관계없이 동일한 인터페이스로 사용합니다.

```java
KtasLevelWeight weightStrategy = KtasLevelWeight.fromLevel(patientInfo.getKtas().getLevel());
double baseScore = weightStrategy.calculateBaseScore(capabilityScore, etaScore);
```

### Redis 기반 거절 쿨다운 시스템

병원이 특정 증상으로 거절했을 때, 거절 사유에 따라 서로 다른 TTL로 Redis에 쿨다운을 저장합니다. 다음 탐색에서 해당 병원은 -1000점 페널티를 받아 후순위로 밀려납니다.

| 거절 사유 | 쿨다운 TTL | 근거 |
|----------|-----------|------|
| 병상 부족 | 30분 | 병상 회전이 빠름 |
| 의료 장비 사용 중 | 90분 | CT/MRI 등 평균 사용 시간 |
| 의료진 부재 | 4시간 | 교대 근무 시까지 변화 없음 |

### 설명 가능한 AI (XAI)

모든 점수 계산 근거를 `score_details` JSON에 저장하여, 구급대원이 "왜 이 병원이 추천되었는지" 확인할 수 있도록 했습니다.

```json
{
  "eta": 420,
  "distance": 3200,
  "matchingCapabilities": ["뇌졸중센터(혈전용해)", "MRI"],
  "cooldownAppliedAt": null
}
```

### 증상별 진료 적합도 매핑 (`symptom-rules.yaml`)

의료 전문 지식을 코드와 분리하여 YAML 파일로 외부화했습니다. '119구급대원 현장응급처치 표준지침'을 기반으로 증상별 필수 진료 자원과 가산점 항목을 정의합니다.

```yaml
- symptom_keyword: "neuro_stroke"
  description: "급성 뇌졸중 의증"
  api_filters:
    typeCode: ["A", "C"]       # 지역응급의료센터 이상 (119지침 근거)
    svdssCd: ["Y0020", "Y0031"] # 뇌경색재관류, 뇌출혈수술 가능 여부
    rltmMeCd: ["O027", "O028"]  # CT 또는 MRI 보유 여부
  scoring_rules:
    - { code: "O028", points: 20 } # MRI 보유 시 가산점
```

---

## 7. 데이터베이스 설계

### ERD
> ERD 이미지 추가 예정 (V2 기준)

### 핵심 테이블 관계
```
incidents (응급 상황)
  └── search_sessions (탐색 시도, 1:N — 재탐색 이력 관리)
        └── hospital_rankings (우선순위 결과, 1:N)
              └── inquiry_logs (통화 문의 기록, 1:N — 재시도 이력 관리)

call_artifacts (통화 결과물, MongoDB)
  └── inquiry_id로 inquiry_logs와 연결
```

- **`score_details`**: 각 병원의 점수 근거를 JSON으로 저장 (XAI)
- **`review_logs`**: `target_type` 필드로 다형적 관계 구현 (DB 변경 없이 확장 가능)

---

## 8. API 문서

- [api-docs.json](./docs/api-docs.json)

---

## 9. 관련 레포지토리

- [Medicall-Client](https://github.com/gdsc-konkuk/24-25-proj-Atempo-Client) — 구급대원 앱 (Flutter)
- [Medicall-AI](https://github.com/gdsc-konkuk/24-25-proj-Atempo-AI) — AI Gateway 서버

---

> 설계 의사결정 및 V1→V2 아키텍처 변경 이유는 [ARCHITECTURE.md](./docs/ARCHITECTURE.md)를 참고하세요.
