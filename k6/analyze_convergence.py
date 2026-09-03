#!/usr/bin/env python3
# k6 -o json=... 로 저장한 원시 포인트 로그를 읽어, create_rankings_duration의
# 누적 p95가 표본이 쌓일수록 어떻게 수렴하는지 분석한다.
#
# 판정 기준(AND 조건): 아래 둘을 모두 만족하는 첫 체크포인트를 "안정화 지점"으로 채택한다.
#   (a) 표본 수 n >= MIN_SAMPLES — 퍼센타일 자체가 신뢰를 가지려면 필요한 절대적 하한.
#       이게 없으면 우연히 변화율이 작았던 초반 구간(예: n=40)을 안정화로 오판할 수 있다
#       (표본이 적을수록 p95는 최댓값 근처 한두 개 값에 좌우되어 원래 불안정하다).
#   (b) 연속된 체크포인트(20개 단위) 간 p95 변화율이 ±5% 이내인 상태가 3회 연속 유지
#
# 사용법: python3 k6/analyze_convergence.py k6/results/pilot-raw.json

import sys
import re
import json
import math
from datetime import datetime

# Python 3.9의 fromisoformat은 소수점 이하 자릿수가 3 또는 6이 아니면 파싱에 실패한다.
# k6가 남기는 타임스탬프는 자릿수가 들쭉날쭉하므로(예: .84066), 6자리로 정규화한다.
_FRACTION_RE = re.compile(r"\.(\d+)")


def normalize_iso(ts):
    def pad(m):
        return "." + m.group(1)[:6].ljust(6, "0")
    return _FRACTION_RE.sub(pad, ts)

METRIC_NAME = "create_rankings_duration"
CHECKPOINT_STEP = 20
TOLERANCE = 0.05
REQUIRED_CONSECUTIVE = 3
MIN_SAMPLES = 100


def percentile(sorted_values, p):
    n = len(sorted_values)
    idx = max(0, math.ceil(p * n) - 1)
    idx = min(idx, n - 1)
    return sorted_values[idx]


def load_points(path):
    points = []
    with open(path) as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            row = json.loads(line)
            if row.get("type") != "Point" or row.get("metric") != METRIC_NAME:
                continue
            data = row["data"]
            ts = datetime.fromisoformat(normalize_iso(data["time"]))
            points.append((ts, data["value"]))
    points.sort(key=lambda x: x[0])
    return points


def main():
    if len(sys.argv) != 2:
        print(f"usage: {sys.argv[0]} <k6-raw-json-path>")
        sys.exit(1)

    points = load_points(sys.argv[1])
    if not points:
        print(f"{METRIC_NAME}에 대한 포인트를 찾지 못했습니다.")
        sys.exit(1)

    t0 = points[0][0]
    values_so_far = []
    checkpoints = []  # (n, elapsed_sec, p95)

    for i, (ts, value) in enumerate(points, start=1):
        values_so_far.append(value)
        if i % CHECKPOINT_STEP == 0:
            elapsed = (ts - t0).total_seconds()
            p95 = percentile(sorted(values_so_far), 0.95)
            checkpoints.append((i, elapsed, p95))

    print(f"{'n':>6} {'elapsed(s)':>12} {'p95(ms)':>10} {'변화율':>8} {'consec':>7}")
    consecutive = 0
    prev_p95 = None
    consecutive_at = []  # 체크포인트별 그 시점까지의 consecutive 값
    for n, elapsed, p95 in checkpoints:
        if prev_p95 is None:
            change_str = "-"
        else:
            change = abs(p95 - prev_p95) / prev_p95
            change_str = f"{change*100:5.1f}%"
            if change <= TOLERANCE:
                consecutive += 1
            else:
                consecutive = 0
        consecutive_at.append(consecutive)
        print(f"{n:>6} {elapsed:>12.1f} {p95:>10.1f} {change_str:>8} {consecutive:>7}")
        prev_p95 = p95

    # (a) n >= MIN_SAMPLES 이고 (b) consecutive >= REQUIRED_CONSECUTIVE 인 첫 체크포인트를 채택
    stabilized_at = None
    for (n, elapsed, p95), consec in zip(checkpoints, consecutive_at):
        if n >= MIN_SAMPLES and consec >= REQUIRED_CONSECUTIVE:
            stabilized_at = (n, elapsed, p95)
            break

    print()
    if stabilized_at:
        n, elapsed, p95 = stabilized_at
        print(f"안정화 지점: n*={n} (>= 최소 표본 {MIN_SAMPLES}), t*={elapsed:.1f}초 (p95={p95:.1f}ms)")
        print(f"권장 duration (VU=10 기준, 여유 포함 반올림): {math.ceil(elapsed / 10) * 10}초")
    else:
        total_elapsed = (points[-1][0] - t0).total_seconds()
        print(f"파일럿 구간 내({total_elapsed:.1f}초, n={len(points)}) 두 조건을 동시에 만족하지 못했습니다.")
        print("파일럿 duration을 늘리거나 임계값을 조정해야 합니다.")


if __name__ == "__main__":
    main()
