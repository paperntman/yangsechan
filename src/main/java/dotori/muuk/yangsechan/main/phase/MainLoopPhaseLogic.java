package dotori.muuk.yangsechan.main.phase;

import dotori.muuk.yangsechan.main.Game;
import dotori.muuk.yangsechan.util.NodEvent;
import dotori.muuk.yangsechan.util.NodStatus;
import dotori.muuk.yangsechan.util.ShakeEvent;
import dotori.muuk.yangsechan.util.VoteScoreboardManager;
import dotori.muuk.yangsechan.util.VoteManager;
import dotori.muuk.yangsechan.util.VoteResultListener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

public class MainLoopPhaseLogic implements PhaseLogic, VoteResultListener {

    private final Game game;
    private Queue<Player> turnQueue;
    private Player currentTarget;
    private Collection<Player> successfulPlayers;

    // --- 심판 모드 관련 변수 ---
    private VoteManager judgementVote;
    private VoteScoreboardManager scoreboardManager;
    private boolean isJudgementMode = false;
    private final Object judgementLock = new Object();

    public MainLoopPhaseLogic(Game game) {
        this.game = game;
    }

    @Override
    public void onEnter() {
        this.turnQueue = new LinkedList<>(game.getPlayers());
        successfulPlayers = new ArrayList<>();
        game.broadCast(Component.text("메인 게임을 시작합니다! 자신의 단어를 맞춰보세요!", NamedTextColor.AQUA));
        nextTurn();
    }

    @Override
    public void onExit() {
        if (currentTarget != null) {
            currentTarget.resetTitle();
        }
        // 이 페이즈는 게임이 끝날 때까지 유지되므로, onExit은 게임 종료 시점에 호출됨
        game.broadCast(Component.text("모든 플레이어가 정답을 맞혔습니다! 게임 종료!", NamedTextColor.GOLD));
    }

    private void nextTurn() {
        // 성공한 플레이어는 턴 큐에서 완전히 제거
        turnQueue.removeIf(successfulPlayers::contains);

        if (turnQueue.isEmpty()) {
            game.setPhase(GamePhase.ENDED);
            return;
        }

        // 다음 턴 플레이어 설정 (순환 큐)
        this.currentTarget = turnQueue.poll();
        turnQueue.offer(this.currentTarget);

        // 질문 모드로 전환 및 화면 업데이트
        switchToQuestionMode();
    }

    /**
     * 현재 턴의 플레이어 화면을 '질문 모드'에 맞게 업데이트합니다.
     */
    private void switchToQuestionMode() {
        this.isJudgementMode = false;
        String targetWord = game.getPlayerWord(currentTarget);
        Title.Times times = Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(10), Duration.ofMillis(800));

        // TARGET에게 질문 유도
        Component targetTitle = Component.text("당신의 차례입니다!", NamedTextColor.YELLOW);
        Component targetSubtitle = Component.text("질문하세요! 턴을 넘기려면 고개를 끄덕이세요.", NamedTextColor.GRAY);
        currentTarget.showTitle(Title.title(targetTitle, targetSubtitle, times));

        // 다른 플레이어들에게 TARGET의 단어 정보 제공
        List<Player> others = new ArrayList<>(game.getPlayers());
        others.remove(currentTarget);
        Component othersTitle = Component.text(currentTarget.getName(), NamedTextColor.GOLD, TextDecoration.BOLD).append(Component.text(" 님의 차례", NamedTextColor.WHITE));
        Component othersSubtitle = Component.text("정답: ", NamedTextColor.GRAY).append(Component.text(targetWord, NamedTextColor.AQUA));
        game.broadCastTitle(Title.title(othersTitle, othersSubtitle, times), others);
    }

    /**
     * '정답 심판 모드'를 시작합니다.
     */
    private void startJudgementMode() {
        synchronized (judgementLock) { // 동기화 시작
            this.isJudgementMode = true;

            List<Player> voters = new ArrayList<>(game.getPlayers());
            voters.remove(currentTarget);

            this.judgementVote = new VoteManager(voters, this);
            this.scoreboardManager = new VoteScoreboardManager(game.getGameManager().getPlugin());
            this.scoreboardManager.createScoreboard(voters);
            this.scoreboardManager.updateScoreboard(judgementVote.getVoteStatus());
        } // 동기화 블록 바깥에서 타이틀/메시지 전송

        Title.Times times = Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(15), Duration.ofMillis(800));
        Component title = Component.text("심판 시간!", NamedTextColor.RED);
        Component subtitle = Component.text(currentTarget.getName() + " 님이 정답을 외칩니다!", NamedTextColor.WHITE);
        game.broadCastTitle(Title.title(title, subtitle, times), game.getPlayers());

        currentTarget.sendMessage(Component.text("정답을 마이크에 대고 외쳐주세요!", NamedTextColor.GREEN));
    }

    /**
     * 심판 모드와 관련된 모든 리소스를 정리합니다.
     */
    private void cleanupJudgementMode() {
        synchronized (judgementLock) { // 동기화 시작
            this.isJudgementMode = false;
            if (judgementVote != null) {
                judgementVote.cleanup();
                this.judgementVote = null;
            }
            if (scoreboardManager != null) {
                scoreboardManager.cleanup();
                this.scoreboardManager = null;
            }
            // 모든 플레이어의 타이틀을 초기화
            game.getPlayers().forEach(Player::resetTitle);
        } // 동기화 종료
    }

    // --- 이벤트 핸들러 ---


    //TODO 최대 정답 개수
    @Override
    public void onAnswer(Player player) {
        if (player.equals(currentTarget) && !isJudgementMode) {
            startJudgementMode();
        } else if (!player.equals(currentTarget)) {
            player.sendMessage(Component.text("당신의 턴이 아닙니다.", NamedTextColor.RED));
        }
    }

    @Override
    public void onNod(NodEvent event) {
        synchronized (judgementLock) {
            if (isJudgementMode && judgementVote != null) {
                // 1. 투표를 진행합니다. 이 호출로 인해 judgementVote가 null이 될 수 있습니다.
                judgementVote.castVote(event.getPlayer(), NodStatus.NOD);

                // 2. 투표가 끝났는지 확인합니다.
                //    투표가 계속 진행 중일 때(즉, cleanup이 호출되지 않았을 때)만 스코어보드를 업데이트합니다.
                if (judgementVote != null && scoreboardManager != null) {
                    scoreboardManager.updateScoreboard(judgementVote.getVoteStatus());
                }

            } else if (!isJudgementMode) {
                if (event.getPlayer().equals(currentTarget)) {
                    event.getPlayer().sendMessage(Component.text("턴을 넘깁니다.", NamedTextColor.GRAY));
                    nextTurn();
                }
            }
        }
    }

    @Override
    public void onShake(ShakeEvent event) {
        synchronized (judgementLock) {
            if (isJudgementMode && judgementVote != null) {
                // 1. 투표를 진행합니다.
                judgementVote.castVote(event.getPlayer(), NodStatus.SHAKE);

                // 2. 투표가 끝났는지 확인하고, 진행 중일 때만 스코어보드를 업데이트합니다.
                if (judgementVote != null && scoreboardManager != null) {
                    scoreboardManager.updateScoreboard(judgementVote.getVoteStatus());
                }
            }
        }
    }

    @Override
    public void onChat(AsyncChatEvent event) {
        // 메인 루프에서는 특별한 채팅 로직이 필요 없음
    }

    // --- VoteResultListener 구현부 ---

    @Override
    public void onVoteConfirmed(String reason) {
        game.broadCast(Component.text("판정 결과: ", NamedTextColor.WHITE).append(Component.text("정답!", NamedTextColor.GREEN, TextDecoration.BOLD)));
        successfulPlayers.add(currentTarget);

        cleanupJudgementMode();
        nextTurn(); // 다음 턴으로
    }

    @Override
    public void onVoteRejected(String reason) {
        game.broadCast(Component.text("판정 결과: ", NamedTextColor.WHITE).append(Component.text("오답!", NamedTextColor.RED, TextDecoration.BOLD)));
        // 오답 시에는 성공 처리 없이 턴만 넘김

        cleanupJudgementMode();
        nextTurn(); // 다음 턴으로
    }

    @Override
    public void onVoteStatusUpdate(String message) {
        game.broadCast(Component.text(message, NamedTextColor.YELLOW));
    }
}