package dotori.muuk.yangsechan.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 플레이어의 머리 움직임(pitch, yaw) 데이터를 분석하여 특정 제스처(끄덕임, 젓기)를 감지하는 유틸리티 클래스입니다.
 * 이 클래스는 Bukkit API에 의존하지 않는 순수 Java 로직으로 구성됩니다.
 */
public final class HeadMovementAnalyzer {

    // 분석할 데이터 윈도우 크기 (1초 = 20틱). 이 시간 동안의 데이터를 분석합니다.
    public static final int ANALYSIS_WINDOW_SIZE = 10;

    // --- 끄덕임(Nod) 감지 설정 ---
    // 끄덕임으로 인정할 최소 반복 횟수 (ANALYSIS_WINDOW_SIZE 시간 내)
    private static final int NOD_MIN_REPETITIONS = 2;
    // 끄덕임으로 인정할 최소 상하 각도 변화량 (가장 높은 각도와 가장 낮은 각도의 차이)
    private static final float NOD_AMPLITUDE_THRESHOLD = 70.0f;

    // --- 젓기(Shake) 감지 설정 ---
    // 젓기로 인정할 최소 반복 횟수 (ANALYSIS_WINDOW_SIZE 시간 내)
    private static final int SHAKE_MIN_REPETITIONS = 2;
    // 젓기로 인정할 최소 좌우 각도 변화량 (가장 왼쪽 각도와 가장 오른쪽 각도의 차이)
    private static final float SHAKE_AMPLITUDE_THRESHOLD = 50.0f;


    /**
     * 주어진 pitch 데이터 리스트가 연속적인 '끄덕임' 제스처에 해당하는지 분석합니다.
     *
     * @param pitchData 플레이어의 pitch 값 리스트
     * @return 끄덕임 제스처로 판단되면 true, 아니면 false
     */
    public static boolean isNod(List<Float> pitchData) {
        // isGesture 메서드를 호출하여 공통 로직으로 분석
        return isGesture(pitchData, NOD_MIN_REPETITIONS, NOD_AMPLITUDE_THRESHOLD);
    }

    /**
     * 주어진 yaw 데이터 리스트가 연속적인 '젓기' 제스처에 해당하는지 분석합니다.
     * Yaw 값은 -180/180 경계에서 점프하므로, 분석 전 정규화 과정이 필요합니다.
     *
     * @param yawData 플레이어의 yaw 값 리스트
     * @return 젓기 제스처로 판단되면 true, 아니면 false
     */
    public static boolean isShake(List<Float> yawData) {
        if (yawData.size() < ANALYSIS_WINDOW_SIZE) {
            return false;
        }
        // Yaw 데이터는 -180 ~ 180 순환 문제를 해결하기 위해 정규화합니다.
        List<Float> normalizedYaw = normalizeYawData(yawData);
        // isGesture 메서드를 호출하여 공통 로직으로 분석
        return isGesture(normalizedYaw, SHAKE_MIN_REPETITIONS, SHAKE_AMPLITUDE_THRESHOLD);
    }

    /**
     * 제스처를 분석하는 핵심 로직입니다.
     * 데이터에서 극값(봉우리/골짜기)을 찾아 반복 횟수와 진폭을 계산합니다.
     *
     * @param data              분석할 데이터 (pitch 또는 정규화된 yaw)
     * @param minRepetitions    제스처로 인정할 최소 반복 횟수
     * @param amplitudeThreshold 제스처로 인정할 최소 진폭
     * @return 제스처 조건을 만족하면 true
     */
    private static boolean isGesture(List<Float> data, int minRepetitions, float amplitudeThreshold) {
        if (data.size() < ANALYSIS_WINDOW_SIZE) {
            return false;
        }

        // 1. 데이터에서 극값(local extrema, 즉 봉우리와 골짜기)을 찾습니다.
        //    이는 단순한 방향 전환보다 더 안정적으로 움직임의 '의도'를 파악하게 해줍니다.
        List<Float> extrema = findExtrema(data);

        // 2. 충분한 반복이 있었는지 확인합니다.
        //    예: [위, 아래, 위] 움직임은 3개의 극값을 가집니다.
        if (extrema.size() < minRepetitions + 1) {
            return false;
        }

        // 3. 움직임의 크기(진폭)가 충분한지 확인합니다.
        float amplitude = Collections.max(extrema) - Collections.min(extrema);
        return amplitude >= amplitudeThreshold;
    }

    /**
     * 데이터 리스트에서 지역적 극값(봉우리와 골짜기)을 찾아 리스트로 반환합니다.
     *
     * @param data 분석할 데이터
     * @return 극값들의 리스트
     */
    private static List<Float> findExtrema(List<Float> data) {
        List<Float> extrema = new ArrayList<>();
        if (data.size() < 3) {
            return extrema;
        }

        // 첫 번째 데이터 포인트는 이전 값이 없으므로 극값 후보로 추가
        extrema.add(data.get(0));

        for (int i = 1; i < data.size() - 1; i++) {
            float prev = data.get(i - 1);
            float current = data.get(i);
            float next = data.get(i + 1);

            // 봉우리(peak) 감지
            if (prev < current && current > next) {
                extrema.add(current);
            }
            // 골짜기(trough) 감지
            else if (prev > current && current < next) {
                extrema.add(current);
            }
        }

        // 마지막 데이터 포인트는 다음 값이 없으므로 극값 후보로 추가
        extrema.add(data.get(data.size() - 1));
        return extrema;
    }

    /**
     * Yaw 값의 순환(-180/180 점프)을 보정하여 연속적인 데이터로 만듭니다.
     * 예를 들어, 170도에서 -170도로 변하는 움직임은 실제로는 20도 만큼의 작은 움직임입니다.
     * 이 메서드는 이러한 점프를 보정하여 [170, 190] 과 같은 연속적인 값으로 변환합니다.
     *
     * @param rawYawData 원본 Yaw 데이터
     * @return 점프가 보정된 연속적인 Yaw 데이터
     */
    private static List<Float> normalizeYawData(List<Float> rawYawData) {
        List<Float> normalized = new ArrayList<>();
        if (rawYawData.isEmpty()) {
            return normalized;
        }

        float offset = 0;
        normalized.add(rawYawData.get(0));

        for (int i = 1; i < rawYawData.size(); i++) {
            float prevRaw = rawYawData.get(i - 1);
            float currentRaw = rawYawData.get(i);
            float diff = currentRaw - prevRaw;

            // -180 -> 180 (실제로는 작은 우회전) 과 같은 큰 양의 점프 감지
            if (diff > 180.0f) {
                offset -= 360.0f;
            }
            // 180 -> -180 (실제로는 작은 좌회전) 과 같은 큰 음의 점프 감지
            else if (diff < -180.0f) {
                offset += 360.0f;
            }
            normalized.add(currentRaw + offset);
        }
        return normalized;
    }
}