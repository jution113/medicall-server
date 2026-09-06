package juton113.Medicall.domain.hospitalRanking;

import juton113.Medicall.domain.hospital.Hospital;
import juton113.Medicall.domain.hospital.HospitalService;
import juton113.Medicall.domain.hospitalRanking.dto.InquiryConfirmUpdateRequest;
import juton113.Medicall.domain.hospitalRanking.dto.ReviewInfo;
import juton113.Medicall.domain.hospitalRanking.dto.ScoreDetails;
import juton113.Medicall.domain.hospitalRanking.enums.*;
import juton113.Medicall.domain.incident.dto.*;
import juton113.Medicall.domain.incident.enums.SymptomKeyword;
import juton113.Medicall.domain.inquiryLog.InquiryLog;
import juton113.Medicall.domain.inquiryLog.InquiryLogService;
import juton113.Medicall.domain.inquiryLog.enums.RejectionReason;
import juton113.Medicall.domain.reviewLog.ReviewLog;
import juton113.Medicall.domain.reviewLog.ReviewLogService;
import juton113.Medicall.domain.reviewLog.enums.ReviewTargetType;
import juton113.Medicall.domain.searchSession.SearchSession;
import juton113.Medicall.global.config.SymptomRuleConfig;
import juton113.Medicall.global.exception.CustomException;
import juton113.Medicall.global.external.KakaoMobilityService;
import juton113.Medicall.global.external.NemcApiService;
import juton113.Medicall.global.external.aiGateway.MlServerService;
import juton113.Medicall.global.external.aiGateway.dto.AiBulkResponse;
import juton113.Medicall.global.external.aiGateway.dto.AiHospitalInfo;
import juton113.Medicall.global.external.aiGateway.dto.AiPatientInfo;
import juton113.Medicall.global.external.aiGateway.dto.AiPenaltyInfo;
import juton113.Medicall.global.external.dto.EtaResult;
import juton113.Medicall.global.sse.SseService;
import juton113.Medicall.global.sse.dto.InquiryConfirmStatusEvent;
import juton113.Medicall.global.util.RedisService;
import juton113.Medicall.global.util.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class HospitalRankingService {
    private final KakaoMobilityService kakaoMobilityService;
    private final NemcApiService nemcApiService;
    private final HospitalService hospitalService;
    private final InquiryLogService inquiryLogService;
    private final MlServerService mlServerService;
    private final RedisService redisService;
    private final ReviewLogService reviewLogService;
    private final SseService sseService;

    private final SymptomRuleConfig symptomRuleConfig;

    private final HospitalRankingRepository hospitalRankingRepository;

    private record RankingBuildContext(
            HospitalRanking hospitalRanking,
            Hospital hospital,
            int duration,
            int distance,
            double finalScore,
            List<String> capabilities,
            Boolean isWarning,
            String deselectionReason,
            String cooldownAppliedAt,
            Long cooldownDurationSeconds) {
    }

    @Transactional
    public List<HospitalRankingInfo> createRankings(SearchSession searchSession,
                                                    PatientInformation patientInfo,
                                                    Location origin) {
        SymptomKeyword symptomKeyword = patientInfo.getSymptomKeyword();
        SymptomRuleConfig.SymptomRule rule = symptomRuleConfig.getRule(symptomKeyword);
        SymptomRuleConfig.ApiFilters filters = rule.getApiFilters();

        // 1. 실시간 병원 정보 조회
        String asort = filters.getTypeCode().isEmpty() ? "A,C,D" : String.join(",", filters.getTypeCode());
        String rltmEmerCd = filters.getRltmEmerCd().isEmpty() ? "O001" : String.join(",", filters.getRltmEmerCd());
        String rltmCd = String.join(",", filters.getRltmCd());
        String svdssCd = String.join(",", filters.getSvdssCd());
        String rltmMeCd = String.join(",", filters.getRltmMeCd());
        String subTypes = String.join(",", filters.getSubTypes());

        List<NemcApiService.NemcHospitalData> hospitalDatas = nemcApiService.getRealtimeHospitalData(
                asort,
                rltmEmerCd,
                rltmCd,
                svdssCd,
                rltmMeCd,
                subTypes,
                origin,
                searchSession.getSearchRadius()
        );

        // 2. AI 서버 일괄(Bulk) 조회

        // 2.1 AI에 보낼 요청 데이터(DTO) 리스트 생성
        List<AiHospitalInfo> aiRequestList = new ArrayList<>();
        for (NemcApiService.NemcHospitalData hospitalData : hospitalDatas) {
            String hospitalCode = hospitalData.getHospitalCode();

            // TODO: [성능 저하 지점] MVP에서는 Redis N+1 조회 허용, (나중에 이 부분도 bulkGet으로 리팩토링 필요)
            // NemcData의 메시지를 String 리스트로 변환 (null-safe)
            List<String> erMessages = Optional.ofNullable(hospitalData.getErMessages())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(NemcApiService.NemcMessage::getMessage)
                    .collect(Collectors.toList());

            List<String> unavailableMessages = Optional.ofNullable(hospitalData.getUnavailableMessages())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(NemcApiService.NemcUnavailableMessage::getMessage)
                    .collect(Collectors.toList());

            aiRequestList.add(AiHospitalInfo.builder()
                    .hospitalCode(hospitalCode)
                    .erMessages(erMessages)
                    .unavailableMessages(unavailableMessages)
                    .build());
        }

        // 2.2 AI 서버에 일괄 요청 (API 1회)
        AiBulkResponse aiResponse = mlServerService.predictRejectionFromMessages(
                AiPatientInfo.from(patientInfo),
                aiRequestList
        );

        // 2.3 AI 응답 결과를 Map으로 변환 (빠른 조회를 위해)
        Map<String, AiPenaltyInfo> penaltyMap = aiResponse.getHospitalList().stream()
                .collect(Collectors.toMap(AiPenaltyInfo::getHospitalCode, info -> info));

        // 3. 병원 / 최근 문의 수 벌크 조회
        List<String> hospitalCodes = hospitalDatas.stream()
                .map(NemcApiService.NemcHospitalData::getHospitalCode)
                .toList();

        Map<String, Hospital> hospitalByCode = hospitalService.findHospitals(hospitalCodes).stream()
                .collect(Collectors.toMap(Hospital::getCode, hospital -> hospital));

        Map<String, Long> recentInquiryCountByCode = inquiryLogService.getRecentInquiryCounts(
                hospitalCodes,
                Duration.ofHours(1)
        );

        Map<String, RedisService.CooldownData> cooldownByCode = redisService.getActiveCooldownInfos(
                hospitalCodes,
                symptomKeyword.getKeyword()
        );

        // 4. 점수 계산
        List<RankingBuildContext> buildContexts = new ArrayList<>();

        // 거리 조회(Kakao)는 N+1 MVP에서 허용, 나머지(병원 조회/문의 수/거절 이력)는 위에서 벌크 조회
        for (NemcApiService.NemcHospitalData hospitalData : hospitalDatas) {
            Hospital hospital = hospitalByCode.get(hospitalData.getHospitalCode());
            if (hospital == null) continue;

            String hospitalCode = hospital.getCode();

            // 4.1. 기본 점수 계산
            // 4.1.1. 진료 적합도 계산
            double capabilityScore = calculateCapabilityScore(rule.getScoringRules(), hospitalData);

            // 4.1.2. 거리 점수 계산
            Location dest = hospital.getLocation();
            EtaResult eta = kakaoMobilityService.getEtaAndDistance(
                    origin.getLongitude(),
                    origin.getLatitude(),
                    dest.getLongitude(),
                    dest.getLatitude()
            );
            int duration = eta.getDuration();
            int distance = eta.getDistance();

            double etaScore = Math.max(0, 100 - ((duration / 60.0) * 2.0)); // 단위: 초

            KtasLevelWeight weightStrategy = KtasLevelWeight.fromLevel(patientInfo.getKtas().getLevel());
            double baseScore = weightStrategy.calculateBaseScore(capabilityScore, etaScore);

            // 4.1.3. 병상 점수 계산
            double bedAvailabilityScore = calculateBedScore(hospitalData);

            // 4.2. 패널티 계산
            // 4.2.1. 이전 문의 수 패널티
            long inquiriesLastHour = recentInquiryCountByCode.getOrDefault(hospitalCode, 0L);
            double rejectionCountPenalty = (double) (inquiriesLastHour * -50);

            String deselectionReason = null;
            String cooldownAppliedAt = null;
            Long cooldownDurationSeconds = null;

            double rejectionHistoryPenalty = 0.0;
            double unavailableMessagePenalty = 0.0;

            Boolean isWarning = false;

            // 4.2.2. 이전 거절 이력 패널티 계산
            RedisService.CooldownData cooldownData = cooldownByCode.get(hospitalCode);
            if (cooldownData != null) {
                RejectionReason rejectionReason = cooldownData.getReason();
                isWarning = true;

                deselectionReason = rejectionReason.getDescription();
                cooldownAppliedAt = cooldownData.getAppliedAt().toString();
                cooldownDurationSeconds = rejectionReason.getCooldownDuration().toSeconds();

                rejectionHistoryPenalty = -1000.0;
            } else {
                // 4.2.3. 병원 거절 메시지 페널티 계산
                AiPenaltyInfo aiPenalty = penaltyMap.getOrDefault(hospitalCode, AiPenaltyInfo.empty());

                deselectionReason = aiPenalty.getPenaltyReason().isEmpty() ? null : aiPenalty.getPenaltyReason();
                unavailableMessagePenalty = convertProbabilityToScore(aiPenalty.getRejectionProbability(), -500.0);
            }

            // 4.3. 최종 점수 계산
            double finalScore = baseScore
                    + bedAvailabilityScore
                    + rejectionCountPenalty
                    + rejectionHistoryPenalty
                    + unavailableMessagePenalty;

            // 진료 적합도
            List<String> capabilities = getMatchingCapabilities(rule.getScoringRules(), hospitalData);

            ScoreDetails scoreDetails = ScoreDetails.from(duration,
                    distance,
                    capabilities,
                    cooldownAppliedAt,
                    cooldownDurationSeconds);

            HospitalRanking hospitalRanking = HospitalRanking.from(searchSession,
                    hospital,
                    finalScore,
                    scoreDetails,
                    deselectionReason);

            buildContexts.add(new RankingBuildContext(
                    hospitalRanking,
                    hospital,
                    duration,
                    distance,
                    finalScore,
                    capabilities,
                    isWarning,
                    deselectionReason,
                    cooldownAppliedAt,
                    cooldownDurationSeconds));
        }

        // 순위가 매겨진 병원 일괄 저장
        hospitalRankingRepository.saveAll(buildContexts.stream()
                .map(RankingBuildContext::hospitalRanking)
                .toList());

        // 5. 응답 생성
        List<HospitalRankingInfo> response = new ArrayList<>();
        for (RankingBuildContext context : buildContexts) {
            HospitalInfo hospitalInfo = HospitalInfo.from(context.hospital(), context.duration(), context.distance());
            RankingInfo rankingInfo = RankingInfo.from(
                    context.hospitalRanking(),
                    context.finalScore(),
                    context.capabilities(),
                    context.isWarning(),
                    context.deselectionReason(),
                    context.cooldownAppliedAt(),
                    context.cooldownDurationSeconds(),
                    false);

            response.add(HospitalRankingInfo.from(
                    context.hospitalRanking().getHospitalRankingId(),
                    hospitalInfo,
                    new ArrayList<>(),
                    rankingInfo,
                    null)
            );
        }

        // 6. response 정렬
        response.sort(Comparator.comparingDouble((HospitalRankingInfo info) -> info.getRankingInfo().getScore())
                .reversed());

        for (int i = 0; i < response.size(); i++) {
            RankingInfo rankingInfo = response.get(i).getRankingInfo();
            int rank = i + 1;

            HospitalRanking hospitalRanking = hospitalRankingRepository.findById(rankingInfo.getRankingId()).orElseThrow(() -> new CustomException(ErrorCode.HOSPITAL_RANKING_NOT_FOUND));
            hospitalRanking.setRank(rank);
            rankingInfo.setRank(rank);
        }

        return response;
    }

    private double calculateCapabilityScore(SymptomRuleConfig.ScoringRuleMap scoringRuleMap,
                                            NemcApiService.NemcHospitalData hospitalData) {
        double score = 0;

        Map<String, NemcApiService.NemcAvailabilityInfo> rltmCdElements = Optional.ofNullable(hospitalData.getRltmCd()).map(NemcApiService.NemcElementMap::getElements).orElse(Collections.emptyMap());
        Map<String, NemcApiService.NemcAvailabilityInfo> rltmMeCdElements = Optional.ofNullable(hospitalData.getRltmMeCd()).map(NemcApiService.NemcElementMap::getElements).orElse(Collections.emptyMap());
        Map<String, NemcApiService.NemcAvailabilityInfo> svdssCdElements = Optional.ofNullable(hospitalData.getSvdssCd()).map(NemcApiService.NemcElementMap::getElements).orElse(Collections.emptyMap());
        Map<String, NemcApiService.NemcAvailabilityInfo> rltmEmerCdElements = Optional.ofNullable(hospitalData.getRltmEmerCd()).map(NemcApiService.NemcElementMap::getElements).orElse(Collections.emptyMap());
        List<String> subTypes = Optional.ofNullable(hospitalData.getSubTypes()).orElse(Collections.emptyList());

        // 1. rltmCd 가산점 계산
        for (SymptomRuleConfig.ScoringRuleItem item : scoringRuleMap.getRltmCd()) {
            if (isResourceAvailable(rltmCdElements.get(item.getCode()))) score += item.getPoints();
        }

        // 2. rltmMeCd 가산점 계산
        for (SymptomRuleConfig.ScoringRuleItem item : scoringRuleMap.getRltmMeCd()) {
            if (isResourceAvailable(rltmMeCdElements.get(item.getCode()))) score += item.getPoints();
        }

        // 3. svdssCd 가산점 계산
        for (SymptomRuleConfig.ScoringRuleItem item : scoringRuleMap.getSvdssCd()) {
            if (isResourceAvailable(svdssCdElements.get(item.getCode()))) score += item.getPoints();
        }

        // 4. rltmEmerCd 가산점 계산
        for (SymptomRuleConfig.ScoringRuleItem item : scoringRuleMap.getRltmEmerCd()) {
            if (isResourceAvailable(rltmEmerCdElements.get(item.getCode()))) score += item.getPoints();
        }

        // 5. subTypes 가산점 계산
        for (SymptomRuleConfig.ScoringRuleItem item : scoringRuleMap.getSubTypes()) {
            if (subTypes.contains(item.getCode())) score += item.getPoints();
        }

        return score;
    }

    private List<String> getMatchingCapabilities(SymptomRuleConfig.ScoringRuleMap scoringRuleMap,
                                                 NemcApiService.NemcHospitalData hospitalData) {

        List<String> capabilities = new ArrayList<>();

        Map<String, NemcApiService.NemcAvailabilityInfo> rltmCdElements = Optional.ofNullable(hospitalData.getRltmCd()).map(NemcApiService.NemcElementMap::getElements).orElse(Collections.emptyMap());
        Map<String, NemcApiService.NemcAvailabilityInfo> rltmMeCdElements = Optional.ofNullable(hospitalData.getRltmMeCd()).map(NemcApiService.NemcElementMap::getElements).orElse(Collections.emptyMap());
        Map<String, NemcApiService.NemcAvailabilityInfo> svdssCdElements = Optional.ofNullable(hospitalData.getSvdssCd()).map(NemcApiService.NemcElementMap::getElements).orElse(Collections.emptyMap());
        List<String> subTypes = Optional.ofNullable(hospitalData.getSubTypes()).orElse(Collections.emptyList());

        // 1. rltmCd (병상) 확인
        for (SymptomRuleConfig.ScoringRuleItem item : scoringRuleMap.getRltmCd()) {
            if (isResourceAvailable(rltmCdElements.get(item.getCode()))) {
                // RltmCd enum에서 한글 설명(description)을 찾아 추가
                RltmCd enumVal = RltmCd.findByCode(item.getCode());
                if (enumVal != null) capabilities.add(enumVal.getDescription());
            }
        }

        // 2. rltmMeCd (장비) 확인
        for (SymptomRuleConfig.ScoringRuleItem item : scoringRuleMap.getRltmMeCd()) {
            if (isResourceAvailable(rltmMeCdElements.get(item.getCode()))) {
                RltmMeCd enumVal = RltmMeCd.findByCode(item.getCode());
                if (enumVal != null) capabilities.add(enumVal.getDescription());
            }
        }

        // 3. svdssCd (중증질환) 확인
        for (SymptomRuleConfig.ScoringRuleItem item : scoringRuleMap.getSvdssCd()) {
            if (isResourceAvailable(svdssCdElements.get(item.getCode()))) {
                SvdssCd enumVal = SvdssCd.findByCode(item.getCode());
                if (enumVal != null) capabilities.add(enumVal.getDescription());
            }
        }

        // 4. subTypes (병원 특성) 확인
        for (SymptomRuleConfig.ScoringRuleItem item : scoringRuleMap.getSubTypes()) {
            if (subTypes.contains(item.getCode())) {
                HospitalSubType enumVal = HospitalSubType.findByCode(item.getCode());
                if (enumVal != null) capabilities.add(enumVal.getDescription());
            }
        }

        return capabilities;
    }

    private boolean isResourceAvailable(NemcApiService.NemcAvailabilityInfo info) {
        if (info == null || info.getAvailableLevel() == null) return false;

        String level = info.getAvailableLevel().toUpperCase();

        return !level.equals("NONE") && !level.equals("UNAVAILABLE") && !level.startsWith("N");
    }

    private double convertProbabilityToScore(double probability, double maxPenalty) {
        if (probability >= 0.8) {
            return maxPenalty * probability;
        }
        return 0.0;
    }

    private double calculateBedScore(NemcApiService.NemcHospitalData hospitalData) {
        Map<String, NemcApiService.NemcAvailabilityInfo> rltmCdElements = Optional.ofNullable(hospitalData.getRltmCd())
                .map(NemcApiService.NemcElementMap::getElements)
                .orElse(Collections.emptyMap());

        NemcApiService.NemcAvailabilityInfo bedInfo = rltmCdElements.get("O038"); // "입원실" 코드
        int usableBeds = (bedInfo != null && bedInfo.getUsable() != null) ? bedInfo.getUsable() : 0;

        return Math.log10(Math.max(0, usableBeds) + 1);
    }

    public List<HospitalRanking> getHospitalRankingsByIds(List<Long> hospitalRankingIds) {
        List<HospitalRanking> hospitalRankings = hospitalRankingRepository.findAllById(hospitalRankingIds);
        if (hospitalRankings.isEmpty()) {
            throw new CustomException(ErrorCode.HOSPITAL_RANKING_NOT_FOUND);
        }
        return hospitalRankings;
    }

    public List<HospitalRankingInfo> findRankings(Long sessionId) {
        List<HospitalRanking> hospitalRankingList = hospitalRankingRepository.findBySearchSession_searchSessionId(sessionId);
        List<ReviewLog> reviewLogs = reviewLogService.getReviews(sessionId);

        // Key: hospitalRankingId, Value: InquiryLog
        // Key: hospitalRankingId, Value: InquiryLog
        // 수정: 중복 발생 시 기존 값(existing)을 유지합니다.
        Map<Long, InquiryLog> inquiryLogMap = inquiryLogService.getInquiryLogsByRankings(hospitalRankingList)
                .stream()
                .collect(Collectors.toMap(
                        inquiry -> inquiry.getHospitalRanking().getHospitalRankingId(), // Key
                        inquiry -> inquiry,                                             // Value
                        (existing, replacement) -> existing                             // MergeFunction (기존 값 유지)
                ));

        // Key: inquiryLogId, Value: hospitalRankingId
        Map<Long, Long> inquiryIdToRankingIdMap = inquiryLogMap.values().stream()
                .collect(Collectors.toMap(
                        InquiryLog::getInquiryLogId,
                        inquiry -> inquiry.getHospitalRanking().getHospitalRankingId()
                ));

        // Key: hospitalRankingId, Value: List<ReviewLog> (RANKING + INQUIRY 타입 모두)
        Map<Long, List<ReviewLog>> allReviewsByRankingId = reviewLogs.stream()
                .collect(Collectors.groupingBy(
                        review -> {
                            if (review.getReviewTargetType() == ReviewTargetType.RANKING) {
                                // RANKING 타입은 targetId가 곧 hospitalRankingId
                                return review.getTargetId();
                            } else {
                                // INQUIRY 타입은 targetId(inquiryLogId)를 이용해 맵에서 hospitalRankingId 조회
                                return inquiryIdToRankingIdMap.get(review.getTargetId());
                            }
                        }
                ));

        List<HospitalRankingInfo> response = new ArrayList<>();
        String symptomKeyword = hospitalRankingList.get(0).getSearchSession().getIncident().getPatientSymptomKeyword().getKeyword();

        for (HospitalRanking hospitalRanking : hospitalRankingList) {
            long hospitalRankingId = hospitalRanking.getHospitalRankingId();
            Hospital hospital = hospitalRanking.getHospital();
            List<ReviewLog> currentReviews = allReviewsByRankingId.getOrDefault(hospitalRankingId, Collections.emptyList());
            List<ReviewInfo> reviewInfoList = currentReviews.stream()
                    .map(reviewLog -> ReviewInfo.from(
                            reviewLog.getReviewId(),
                            reviewLog.getReviewTargetType(),
                            reviewLog.getReviewStatus(),
                            reviewLog.getComment())
                    )
                    .collect(Collectors.toList());

            ScoreDetails scoreDetails = hospitalRanking.getScoreDetails();

            HospitalInfo hospitalInfo = HospitalInfo.from(
                    hospital,
                    scoreDetails.getEta(),
                    scoreDetails.getDistance());

            RedisService.CooldownData cooldownData = redisService.getActiveCooldownInfo(hospital.getCode(), symptomKeyword);
            Boolean isWarning = false;
            String deselectionReason = hospitalRanking.getDeselectionReason();
            if (hospitalRanking.getDeselectionReason() != null) isWarning = true;

            if (cooldownData != null) {
                RejectionReason rejectionReason = cooldownData.getReason();
                isWarning = true;

                if(deselectionReason == null)
                    deselectionReason = rejectionReason.getDescription();
                else {
                    deselectionReason = deselectionReason.concat(" / " + rejectionReason.getDescription());
                }
            }

            RankingInfo rankingInfo = RankingInfo.from(
                    hospitalRanking,
                    hospitalRanking.getFinalScore(),
                    scoreDetails.getMatchingCapabilities(),
                    isWarning,
                    deselectionReason,
                    scoreDetails.getCooldownAppliedAt(),
                    scoreDetails.getCooldownDurationSeconds(),
                    hospitalRanking.getIsSelectedByUser()
            );

            InquiryResult inquiryResult = inquiryLogService.getInquiryResult(hospitalRankingId);

            response.add(HospitalRankingInfo.from(
                    hospitalRankingId,
                    hospitalInfo,
                    reviewInfoList,
                    rankingInfo,
                    inquiryResult)
            );
        }

        response.sort(Comparator.comparingInt((HospitalRankingInfo info) -> info.getRankingInfo().getRank()));

        return response;
    }

    @Transactional
    public void updateInquiryConfirmStatus(Long sessionId, Long hospitalRankingId, InquiryConfirmUpdateRequest request) {
        HospitalRanking hospitalRanking = hospitalRankingRepository.findById(hospitalRankingId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOSPITAL_RANKING_NOT_FOUND));

        boolean isConfirmedForInquiry = request.isConfirmedForInquiry();

        hospitalRanking.setIsSelectedByUser(isConfirmedForInquiry);

        sseService.sendInquiryConfirmStatus(sessionId, InquiryConfirmStatusEvent.from(hospitalRankingId, isConfirmedForInquiry));
    }
}
