package dotori.muuk.yangsechan.main.phase;

public enum GamePhase {
    RECRUITING,      // 1단계: 플레이어 모집
    WORD_SELECTION,  // 2단계: 단어 선정
    MAIN_LOOP,       // 3단계: 게임 진행 (질문/답변)
    ENDED;           // 게임 종료
}
