# 아키텍처 설계 결정 기록 (Architecture Decision Records)

이 문서는 Medicall V1에서 V2로의 아키텍처 전환 과정에서 내린 주요 설계 결정과 그 근거를 기록합니다.

---

## V1 → V2 변경 개요

| 항목 | V1 | V2 |
|------|----|----|
| 병원 선정 방식 | Gemini AI가 텍스트 기반으로 병원 선택 | KTAS 연동 동적 우선순위 알고리즘 |
| 병원 추천 기준 | AI의 자연어 판단 | 규칙 기반 점수 계산 (XAI) |
| 클라이언트 | Flutter 앱 전용 | Flutter 앱 + 상황실 웹 대시보드 |
| 실시간 통신 | 없음 | SSE (Server-Sent Events) |
| 경로 안내 | Google Maps 연동 | Kakao Mobility API (이송 시간 계산) |
| 데이터 소스 | 정적 병원 데이터 | NEMC 실시간 API + 서울시 공공데이터 |

---

## ADR-001: 병원 선정에 LLM 대신 규칙 기반 알고리즘을 선택한 이유

### 배경

V1에서는 Gemini AI에게 환자 정보와 병원 목록을 전달하고, AI가 자연어로 최적 병원을 판단하도록 설계했습니다.

### 문제

실제 운용을 고려했을 때 세 가지 근본적인 문제가 있었습니다.

**1. 신뢰성 — 비결정적 특성이 생명 시스템에 부적합**

LLM은 동일한 입력에 대해 매번 미세하게 다른 결과를 반환합니다. 응급 의료 시스템에서 "왜 이 병원이 추천되었는가?"라는 질문에 100% 일관된 답을 할 수 없다면, 구급대원이 AI의 판단을 신뢰하기 어렵습니다.

**2. 속도 — 수십 개 병원을 긴급 상황에서 동시 평가**

LLM API 호출은 네트워크 왕복으로 수백ms~수초가 소요됩니다. V2에서는 탐색 반경 내 수십 개 병원을 동시에 평가해야 하는데, LLM 방식으로는 실용적인 응답 시간을 보장할 수 없었습니다.

**3. 설명 가능성 — 구급대원의 신뢰 확보**

"MRI 보유(+20점), 뇌졸중센터 인증(+10점), 이송 시간 8분" 처럼 점수 근거를 투명하게 제시해야 구급대원이 AI 추천을 검토하고 최종 판단을 내릴 수 있습니다. LLM의 자연어 설명은 이 역할을 대체하기 어렵습니다.

### 결정

'119구급대원 현장응급처치 표준지침'을 `symptom-rules.yaml`로 번역하여 외부화하고, 서버 내부에서 수 ms 안에 완료되는 규칙 기반 점수 계산으로 전환했습니다.

의료 전문 지식의 갱신은 YAML 파일만 수정하면 되므로, 엔지니어링 로직과 의료 지식을 완전히 분리했습니다.

---

## ADR-002: KTAS 등급별 가중치를 enum 전략 패턴으로 구현한 이유

### 배경

KTAS(한국형 중증도 분류도구) 등급에 따라 병원 선택 기준이 달라집니다. 가장 단순한 구현은 if-else로 등급별 가중치 숫자만 바꾸는 방식입니다.

```java
// 단순 구현 (채택하지 않음)
if (ktas == 1) {
    score = protocol * 0.1 + time * 0.9;
} else if (ktas == 4 || ktas == 5) {
    score = protocol * 0.5 + time * 0.5;
}
```

### 문제

KTAS 4/5는 단순히 가중치가 다른 게 아닙니다. 비응급 환자가 상급 병원(권역응급센터)으로 집중되면 정작 KTAS 1/2 환자가 받아야 할 자원이 낭비됩니다. 따라서 KTAS 4/5는 **진료 역량이 높은 병원을 오히려 역방향으로 페널티**를 주어야 합니다 (`100 - protocolScore`).

이 로직 차이를 if-else 안에 넣으면, 새로운 등급이 추가되거나 계산 방식이 바뀔 때마다 조건문 전체를 수정해야 하고, 각 등급의 의도가 코드에서 드러나지 않습니다.

### 결정

각 KTAS 등급을 독립적인 전략(Strategy)으로 캡슐화하여, 등급별 계산 책임을 해당 enum 상수 안에 위치시켰습니다.

```java
public enum KtasLevelWeight {
    LEVEL_1(0.1, 0.9) {
        @Override
        public double calculateBaseScore(double protocolScore, double timeScore) {
            // "시간이 생명이다" — 이송 속도 90% 우선
            return (protocolScore * this.getProtocolWeight()) + (timeScore * this.getTimeWeight());
        }
    },
    LEVEL_4_5(0.5, 0.5) {
        @Override
        public double calculateBaseScore(double protocolScore, double timeScore) {
            // "시스템을 보호해야 한다" — 상급 병원 역가중치
            double inverseProtocolScore = 100.0 - protocolScore;
            return (inverseProtocolScore * this.getProtocolWeight()) + (timeScore * this.getTimeWeight());
        }
    };

    public abstract double calculateBaseScore(double protocolScore, double timeScore);
}
```

호출부는 등급에 관계없이 동일한 메서드를 호출하므로, 새로운 KTAS 세분화 기준이 생겨도 호출부 코드는 전혀 변경할 필요가 없습니다.

---

## ADR-003: 병원에 '전화'를 사용하는 이유

### 배경

기존 119 이송 시스템은 병원이 디지털로 수용/거절 의사를 표시하는 방식입니다. 더 효율적으로 보이지만, 실제로는 병원 대부분이 시스템을 무응답으로 방치합니다.

### 원인 분석

병원이 시스템 거절을 회피하는 이유는 응급의료기관 평가 기준에 있습니다.

- **평가 리스크**: 시스템상 '거절'은 NEDIS에 공식 기록되어 `안전성 2-1) 전원의 적절성` 지표에 감점 요인이 됩니다.
- **행정 부담**: 시스템 거절 시 '미수용 환자 관리 대장' 작성 의무가 발생합니다.
- **무응답의 합리성**: 평가 기준집에 '무응답'에 대한 직접적 페널티가 없습니다.

결과적으로 병원 입장에서 '무응답 후 전화로 거절'이 가장 합리적인 선택이 되고, 구급대원이 수십 통의 전화를 직접 돌리는 부담을 떠안게 됩니다.

### 결정

병원의 행동을 바꾸려 하지 않고, 현실을 수용하여 **'전화'라는 소통 방식 자체를 자동화**하는 방향으로 설계했습니다.

Twilio Programmable Voice로 AI 음성봇이 병원에 직접 전화를 걸어 수용 여부와 거절 사유를 파악합니다. 병원 입장에서는 평소와 동일한 전화 통화이고, 구급대원 입장에서는 버튼 하나로 수십 개 병원에 동시 문의가 완료됩니다.

---

## ADR-004: 실시간 통신에 WebSocket 대신 SSE를 선택한 이유

### 배경

통화 진행 상황을 구급대원 앱과 상황실 웹에 실시간으로 전달해야 합니다.

### 결정

**단방향 서버 푸시**가 필요한 시나리오이기 때문에 SSE를 선택했습니다.

- 클라이언트 → 서버: REST API (탐색 시작, 문의 확정 등)
- 서버 → 클라이언트: SSE (병원 우선순위 결과, 통화 상태 실시간 업데이트)

WebSocket은 양방향 통신이 필요할 때 적합하지만, 이 시나리오에서 클라이언트가 서버로 스트리밍 데이터를 보낼 필요가 없습니다. SSE는 HTTP 표준 위에서 동작하므로 프록시·방화벽 호환성이 높고, 자동 재연결을 브라우저 레벨에서 지원합니다.

---

## ADR-005: 멀티 데이터베이스 전략

### 데이터 특성에 따른 DB 선택

**MySQL (AWS RDS)** — 정형 데이터, 트랜잭션 필요
- 응급 상황, 병원 정보, 탐색 세션, 우선순위 결과, 문의 이력
- 각 테이블 간의 관계가 명확하고, 조회 패턴이 예측 가능

**Redis (ElastiCache)** — TTL 기반 임시 데이터, 빠른 조회
- AI 통화 대화 맥락 (통화 종료 후 자동 삭제, TTL 1시간)
- 병원 거절 쿨다운 (사유별 TTL: 30분~4시간)
- 인증 토큰 세션 관리

**MongoDB (DocumentDB)** — 비정형 문서, 대용량 텍스트
- 통화 녹취 전문, AI 요약본, 녹음 파일 URL
- 통화마다 구조가 다를 수 있는 결과물을 유연하게 저장

### RDB ↔ MongoDB 연결 전략

`call_artifacts` 컬렉션의 `inquiry_id` 필드로 MySQL의 `inquiry_logs` 테이블과 연결합니다. 실제 대용량 녹음 파일은 S3에 저장하고, MongoDB에는 URL만 보관하여 DB 성능 저하를 방지했습니다.

---

## ADR-006: 테스트 운영 환경의 인프라 Right-Sizing

### 배경

마스터 문서의 프로덕션 설계안은 3개의 독립 서버(API 서버, AI Gateway 서버, Jenkins CI/CD 서버)와 AWS 완전관리형 서비스(ElastiCache, DocumentDB)로 구성되어 있습니다.

실제 가동 테스트 환경을 구축하면서 운영 비용과 관리 복잡도를 현실적인 수준으로 조정할 필요가 있었습니다.

### 결정 — 유지한 것과 바꾼 것

**유지한 것 (아키텍처 원칙)**

서비스 경계는 그대로 유지했습니다. API 서버와 AI Gateway 서버의 분리는 단순한 서버 개수의 문제가 아니라, 두 서비스가 서로 다른 책임(비즈니스 로직 vs. AI 통화 로직)을 갖는다는 설계 원칙입니다. 이 경계를 합치면 나중에 분리 비용이 훨씬 커지므로 유지했습니다.

**단순화한 것 (인프라 레이어)**

| 구분 | 프로덕션 설계 | 테스트 운영 환경 | 이유 |
|------|-------------|----------------|------|
| CI/CD | Jenkins (별도 EC2) | GitHub Actions | public 레포 무료, 서버 1대 제거, 더 현대적인 선택 |
| NoSQL DB | AWS DocumentDB | MongoDB Atlas Free Tier | DocumentDB 최소 비용 ~$60/month, Atlas 512MB 무료 |
| Cache | AWS ElastiCache | Upstash Redis Free Tier | 테스트 트래픽 수준에서 무료 티어로 충분 |
| 서버 구성 | 3 EC2 | 2 EC2 (t3.small × 2) | Jenkins 제거로 서버 1대 감소 |

**월 예상 비용 비교**

- 프로덕션 설계 기준: ~$150+/month
- 테스트 운영 환경: ~$45/month (EC2 × 2 + RDS)

### 판단 근거

Jenkins 제거는 단순한 비용 절감이 아니라 더 나은 선택이기도 합니다. GitHub Actions는 코드 저장소와 파이프라인이 같은 곳에 있어 가시성이 높고, 별도 Jenkins 서버를 관리할 오버헤드가 없으며, 현재 업계 표준에 더 가깝습니다.

MongoDB Atlas, Upstash Redis는 AWS 완전관리형 서비스와 동일한 인터페이스(MongoDB 드라이버, Redis 프로토콜)를 사용하므로, 실제 프로덕션 전환 시 연결 설정만 교체하면 됩니다. 애플리케이션 코드 변경은 필요하지 않습니다.

### 프로덕션 전환 시 변경 필요 사항

테스트 운영 이후 실제 서비스로 전환한다면 아래 순서로 확장합니다.

1. Upstash Redis → AWS ElastiCache (Redis 프로토콜 호환, 환경변수만 교체)
2. MongoDB Atlas → AWS DocumentDB (MongoDB 드라이버 호환, 환경변수만 교체)
3. GitHub Actions → GitHub Actions 유지 또는 Jenkins 추가 (현재 구성으로도 프로덕션 운영 가능)
4. EC2 t3.small → 트래픽에 따라 스케일업 또는 오토스케일링 그룹 도입
