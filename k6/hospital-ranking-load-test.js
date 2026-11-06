// N+1 개선 전/후 응답시간 비교용 k6 부하테스트.
// perf 프로필로 띄운 서버(FakeKakaoMobility/FakeNemcApi/FakeMlServer/FakeTwilio + medicall_perf DB)를 대상으로 한다.
//
// 공식 측정(파일럿으로 도출한 duration=110초) 실행 예시
// (N=20/40/76 구간별로 재시딩 없이 요청 파라미터만 바꿔서 실행):
//   PHASE=before N=20 k6 run --duration 110s k6/hospital-ranking-load-test.js
//   PHASE=before N=40 k6 run --duration 110s k6/hospital-ranking-load-test.js
//   PHASE=before N=76 k6 run --duration 110s k6/hospital-ranking-load-test.js
//   (Phase 1~6 수정 후 동일하게 PHASE=after 로 반복)
//
// FakeNemcApiService가 search_radius 값을 "반환할 병원 개수"로 재해석하므로, N은 곧 후보 병원 수다.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.4/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const N = Number(__ENV.N || '76');
const PHASE = __ENV.PHASE || 'before';

const createRankingsDuration = new Trend('create_rankings_duration', true);
const findRankingsDuration = new Trend('find_rankings_duration', true);

export const options = {
    vus: 10,
    duration: '1m',
};

export function setup() {
    const res = http.post(`${BASE_URL}/api/v1/auth/local/login?role=VERIFIED`);
    check(res, { 'local login 200': (r) => r.status === 200 });
    return { token: res.json('accessToken') };
}

export default function (data) {
    const headers = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${data.token}`,
    };

    const createBody = JSON.stringify({
        location: { latitude: 37.5665, longitude: 126.9780 },
        search_radius: N,
        patient_info: {
            ktas: 1,
            symptom: 'k6 부하테스트용 증상 설명',
            symptom_keyword: 'cardiac_arrest',
            age: 55,
            gender: 'MALE',
            consciousness: 'ALERT',
            vitals: {
                bp_systolic: 120,
                bp_diastolic: 80,
                heart_rate: 90,
                respiratory_rate: 18,
                body_temperature: 36.5,
                spo2: 97,
                blood_sugar_test: 100,
            },
        },
        on_site_treatment: 'CPR 시행 중',
    });

    const createRes = http.post(`${BASE_URL}/api/v1/search-sessions`, createBody, { headers });
    createRankingsDuration.add(createRes.timings.duration);
    const createOk = check(createRes, { 'createRankings 200': (r) => r.status === 200 });

    if (!createOk) {
        sleep(1);
        return;
    }

    const sessionId = createRes.json('search_session_id');

    for (let i = 0; i < 3; i++) {
        const findRes = http.get(`${BASE_URL}/api/v1/search-sessions/${sessionId}/hospital-rankings`, { headers });
        findRankingsDuration.add(findRes.timings.duration);
        check(findRes, { 'findRankings 200': (r) => r.status === 200 });
        sleep(1);
    }
}

export function handleSummary(data) {
    const path = `k6/results/${PHASE}-N${N}.json`;
    return {
        [path]: JSON.stringify(data, null, 2),   // 파일: 원시 데이터 (분석용)
        stdout: textSummary(data, { indent: ' ', enableColors: true }),  // 터미널: 표 형태 (캡처용)
    };
}
