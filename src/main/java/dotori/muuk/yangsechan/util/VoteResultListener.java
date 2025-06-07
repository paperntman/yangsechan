package dotori.muuk.yangsechan.util;

// 투표 결과를 전달받을 객체가 구현해야 하는 인터페이스
public interface VoteResultListener {

    /**
     * 투표가 최종적으로 '확정'되었을 때 호출됩니다.
     * @param reason 확정 사유 (예: "만장일치로", "과반수 찬성으로")
     */
    void onVoteConfirmed(String reason);

    /**
     * 투표가 '기각'되었을 때 호출됩니다.
     * @param reason 기각 사유 (예: "과반수 반대로")
     */
    void onVoteRejected(String reason);

    /**
     * 투표 상황에 변동이 있을 때 알림을 받습니다. (선택사항, UI 업데이트 등에 사용)
     * @param message 현재 투표 상황 메시지 (예: "자동 확정이 유예되었습니다!")
     */
    void onVoteStatusUpdate(String message);
}