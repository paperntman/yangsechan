package dotori.muuk.yangsechan;

import java.util.ArrayList;
import java.util.List;

public class HeadMovementAnalyzer {

    // --- 설정값 (config.yml 등으로 빼면 더 좋습니다) ---

    // 분석할 데이터 윈도우 크기 (2초 = 40틱)
    public static final int ANALYSIS_WINDOW_SIZE = 40;

    // 지속적인 동작으로 인정하기 위한 최소한의 왕복 횟수 (예: 2초 안에 2번 이상 끄덕여야 인정)
    private static final int MIN_REPETITIONS = 4;

    // 끄덕임으로 인정할 최소 상하 각도 변화량 (진폭)
    private static final float NOD_AMPLITUDE_THRESHOLD = 25.0f;

    // 젓기로 인정할 최소 좌우 각도 변화량 (진폭)
    private static final float SHAKE_AMPLITUDE_THRESHOLD = 35.0f;

    /**
     * Pitch 데이터 시퀀스를 분석하여 지속적인 끄덕임인지 판별합니다.
     * @param pitchData 40개의 Pitch 값 리스트
     * @return 끄덕임으로 판별되면 true
     */
    public static boolean isContinuousNod(List<Float> pitchData) {
        // 데이터가 충분하지 않으면 분석하지 않음
        if (pitchData.size() < ANALYSIS_WINDOW_SIZE) {
            return false;
        }
        return hasEnoughRepetitions(pitchData, NOD_AMPLITUDE_THRESHOLD);
    }

    /**
     * Yaw 데이터 시퀀스를 분석하여 지속적인 젓기인지 판별합니다.
     * @param yawData 40개의 Yaw 값 리스트
     * @return 젓기로 판별되면 true
     */
    public static boolean isContinuousShake(List<Float> yawData) {
        if (yawData.size() < ANALYSIS_WINDOW_SIZE) {
            return false;
        }
        // Yaw는 -180 ~ 180을 순환하므로 데이터 정규화가 필요합니다.
        List<Float> normalizedYaw = normalizeYawData(yawData);
        return hasEnoughRepetitions(normalizedYaw, SHAKE_AMPLITUDE_THRESHOLD);
    }

    /**
     * 데이터 시퀀스에서 봉우리(peak)와 골짜기(valley)를 찾아,
     * 최소 반복 횟수와 최소 진폭 조건을 만족하는지 검사합니다.
     *
     * @param data               데이터 시퀀스 (pitch 또는 정규화된 yaw)
     * @param amplitudeThreshold 최소 진폭
     * @return 조건을 만족하면 true
     */
    private static boolean hasEnoughRepetitions(List<Float> data, float amplitudeThreshold) {
        List<Float> peaks = new ArrayList<>();
        List<Float> valleys = new ArrayList<>();

        // 1. 봉우리와 골짜기 찾기
        for (int i = 1; i < data.size() - 1; i++) {
            float prev = data.get(i - 1);
            float current = data.get(i);
            float next = data.get(i + 1);

            if (current > prev && current > next) { // 봉우리(Peak)
                peaks.add(current);
            } else if (current < prev && current < next) { // 골짜기(Valley)
                valleys.add(current);
            }
        }

        // 2. 조건 검사
        // 최소한의 봉우리와 골짜기가 있어야 왕복 운동으로 간주할 수 있음
        if (peaks.size() < HeadMovementAnalyzer.MIN_REPETITIONS || valleys.size() < HeadMovementAnalyzer.MIN_REPETITIONS) {
            return false;
        }

        // 3. 평균 진폭 계산 및 검사
        float totalAmplitude = 0;
        int checks = Math.min(peaks.size(), valleys.size());
        
        // 평균 진폭을 계산하여 노이즈(작은 떨림)를 걸러냅니다.
        for (int i = 0; i < checks; i++) {
             totalAmplitude += Math.abs(peaks.get(i) - valleys.get(i));
        }
        float averageAmplitude = totalAmplitude / checks;

        return averageAmplitude >= amplitudeThreshold;
    }

    /**
     * Yaw 값은 -180에서 180으로 넘어갈 때 큰 값의 점프가 발생합니다. (예: 179 -> -179)
     * 이를 부드럽게 연결하여 분석하기 용이하게 만듭니다.
     */
    private static List<Float> normalizeYawData(List<Float> yawData) {
        List<Float> normalized = new ArrayList<>();
        if (yawData.isEmpty()) return normalized;

        normalized.add(yawData.getFirst());
        float offset = 0;

        for (int i = 1; i < yawData.size(); i++) {
            float prev = yawData.get(i - 1);
            float current = yawData.get(i);
            float diff = current - prev;

            if (diff > 180) { // e.g., -170 -> 170 (실제로는 작은 움직임)
                offset -= 360;
            } else if (diff < -180) { // e.g., 170 -> -170
                offset += 360;
            }
            normalized.add(current + offset);
        }
        return normalized;
    }
}