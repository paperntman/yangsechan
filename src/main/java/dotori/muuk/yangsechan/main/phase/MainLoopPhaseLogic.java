package dotori.muuk.yangsechan.main.phase;

import dotori.muuk.yangsechan.main.Game;
import dotori.muuk.yangsechan.util.NodEvent;
import dotori.muuk.yangsechan.util.NodStatus;
import dotori.muuk.yangsechan.util.ShakeEvent;
import dotori.muuk.yangsechan.util.VoteManager;
import dotori.muuk.yangsechan.util.VoteResultListener;
import dotori.muuk.yangsechan.util.VoteScoreboardManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class MainLoopPhaseLogic implements PhaseLogic, VoteResultListener {

    private final Game game;
    private Queue<Player> turnQueue;
    private Player currentTarget;
    private List<Player> successfulPlayers; // 정답을 맞힌 플레이어 목록

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
        this.successfulPlayers = new ArrayList<>();
        this.turnQueue = new LinkedList<>(game.getPlayers());
        game.initializeAttempts(); // 모든 플레이어의 시도 횟수 초기화
        game.broadCast(Component.text("메인 게임을 시작합니다! 자신의 단어를 맞춰보세요!", NamedTextColor.AQUA));
        nextTurn();
    }

    @Override
    public void onExit() {
        if (currentTarget != null) {
            currentTarget.resetTitle();
        }
    }

    private void nextTurn() {
        if (checkGameEndCondition()) {
            return;
        }

        // 다음 턴 플레이어 설정 (순환 큐)
        do {
            this.currentTarget = turnQueue.poll();
            if (this.currentTarget == null) { // 큐가 비었다면 게임 종료 조건 다시 확인
                if (checkGameEndCondition()) return;
                // 모든 플레이어가 한 바퀴 돌았으므로 큐를 다시 채움
                this.turnQueue = new LinkedList<>(game.getActivePlayers().stream()
                        .filter(p -> !successfulPlayers.contains(p)).toList());
                this.currentTarget = turnQueue.poll();
            }
        } while (game.isEliminated(currentTarget) || successfulPlayers.contains(currentTarget)); // 탈락했거나 성공했으면 건너뜀

        turnQueue.offer(this.currentTarget);
        switchToQuestionMode();
    }

    /**
     * 게임 종료 조건을 확인하고, 종료 조건이 충족되면 게임을 끝냅니다.
     * @return 게임이 종료되면 true, 아니면 false
     */
    private boolean checkGameEndCondition() {
        List<Player> activePlayers = game.getActivePlayers().stream()
                .filter(p -> !successfulPlayers.contains(p))
                .toList();

        if (activePlayers.size() <= 1) { // 살아남은 사람이 1명 이하일 경우
            game.setPhase(GamePhase.ENDED);
            return true;
        }
        return false;
    }

    private void switchToQuestionMode() {
        this.isJudgementMode = false;
        String targetWord = game.getPlayerWord(currentTarget);
        Title.Times times = Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(10), Duration.ofMillis(800));

        // TARGET에게 질문 유도 (남은 기회 표시)
        int remainingAttempts = game.getRemainingAttempts(currentTarget);
        Component targetTitle = Component.text("당신의 차례입니다!", NamedTextColor.YELLOW);
        Component targetSubtitle;

        if (remainingAttempts > 0) {
            targetSubtitle = Component.text("질문하세요! (남은 기회: " + remainingAttempts + "번)\n턴을 넘기려면 고개를 끄덕이세요.", NamedTextColor.GRAY);
        } else {
            targetSubtitle = Component.text("기회를 모두 소진했습니다. 턴을 넘기려면 고개를 끄덕이세요.", NamedTextColor.RED);
        }
        currentTarget.showTitle(Title.title(targetTitle, targetSubtitle, times));

        // 다른 '활동 중인' 플레이어들에게 정보 제공
        List<Player> others = game.getActivePlayers().stream()
                .filter(p -> !p.equals(currentTarget))
                .collect(Collectors.toList());

        Component othersTitle = Component.text(currentTarget.getName(), NamedTextColor.GOLD, TextDecoration.BOLD).append(Component.text(" 님의 차례", NamedTextColor.WHITE));
        Component othersSubtitle = Component.text("정답: ", NamedTextColor.GRAY).append(Component.text(targetWord, NamedTextColor.AQUA));
        game.broadCastTitle(Title.title(othersTitle, othersSubtitle, times), others);
    }

    private void startJudgementMode() {
        synchronized (judgementLock) {
            this.isJudgementMode = true;

            // 투표자는 탈락하지 않은 플레이어 중 본인을 제외한 모두
            List<Player> voters = game.getActivePlayers().stream()
                    .filter(p -> !p.equals(currentTarget))
                    .collect(Collectors.toList());

            if (voters.isEmpty()) { // 심판해줄 사람이 없으면 자동으로 오답처리
                onVoteRejected("심판해줄 플레이어가 없습니다.");
                return;
            }

            this.judgementVote = new VoteManager(voters, this);
            this.scoreboardManager = new VoteScoreboardManager(game.getGameManager().getPlugin());
            this.scoreboardManager.createScoreboard(voters);
            this.scoreboardManager.updateScoreboard(judgementVote.getVoteStatus());
        }

        Title.Times times = Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(15), Duration.ofMillis(800));
        Component title = Component.text("심판 시간!", NamedTextColor.RED);
        Component subtitle = Component.text(currentTarget.getName() + " 님이 정답을 외칩니다!", NamedTextColor.WHITE);
        game.broadCastTitle(Title.title(title, subtitle, times), game.getActivePlayers()); // 활동중인 플레이어에게만 타이틀 표시

        currentTarget.sendMessage(Component.text("정답을 마이크에 대고 외쳐주세요!", NamedTextColor.GREEN));
    }

    private void cleanupJudgementMode() {
        synchronized (judgementLock) {
            this.isJudgementMode = false;
            if (judgementVote != null) {
                judgementVote.cleanup();
                this.judgementVote = null;
            }
            if (scoreboardManager != null) {
                scoreboardManager.cleanup();
                this.scoreboardManager = null;
            }
            game.getActivePlayers().forEach(Player::resetTitle);
        }
    }

    @Override
    public void onAnswer(Player player) {
        if (game.isEliminated(player)) return; // 탈락자는 아무것도 할 수 없음

        if (game.getRemainingAttempts(player) <= 0) {
            player.sendMessage(Component.text("이미 모든 기회를 소진하여 정답을 외칠 수 없습니다.", NamedTextColor.RED));
            return;
        }

        if (player.equals(currentTarget) && !isJudgementMode) {
            startJudgementMode();
        } else if (!player.equals(currentTarget)) {
            player.sendMessage(Component.text("당신의 턴이 아닙니다.", NamedTextColor.RED));
        }
    }

    @Override
    public void onNod(NodEvent event) {
        if (game.isEliminated(event.getPlayer())) return; // 탈락자는 아무것도 할 수 없음

        synchronized (judgementLock) {
            if (isJudgementMode && judgementVote != null) {
                judgementVote.castVote(event.getPlayer(), NodStatus.NOD);
                if (judgementVote != null && scoreboardManager != null) {
                    scoreboardManager.updateScoreboard(judgementVote.getVoteStatus());
                }
            } else if (!isJudgementMode && event.getPlayer().equals(currentTarget)) {
                event.getPlayer().sendMessage(Component.text("턴을 넘깁니다.", NamedTextColor.GRAY));
                nextTurn();
            }
        }
    }

    @Override
    public void onShake(ShakeEvent event) {
        if (game.isEliminated(event.getPlayer())) return; // 탈락자는 아무것도 할 수 없음

        synchronized (judgementLock) {
            if (isJudgementMode && judgementVote != null) {
                judgementVote.castVote(event.getPlayer(), NodStatus.SHAKE);
                if (judgementVote != null && scoreboardManager != null) {
                    scoreboardManager.updateScoreboard(judgementVote.getVoteStatus());
                }
            }
        }
    }

    @Override
    public void onChat(AsyncChatEvent event) {
        // 탈락자의 채팅은 다른 탈락자와 전체 관전자에게만 보이도록 처리 가능 (선택사항)
    }

    @Override
    public void onVoteConfirmed(String reason) {
        game.broadCast(Component.text("판정 결과: ", NamedTextColor.WHITE).append(Component.text("정답!", NamedTextColor.GREEN, TextDecoration.BOLD)), game.getActivePlayers());
        successfulPlayers.add(currentTarget);
        cleanupJudgementMode();
        nextTurn();
    }

    @Override
    public void onVoteRejected(String reason) {
        int remaining = game.decrementAndGetAttempts(currentTarget);

        game.broadCast(Component.text("판정 결과: ", NamedTextColor.WHITE).append(Component.text("오답!", NamedTextColor.RED, TextDecoration.BOLD)), game.getActivePlayers());

        if (remaining > 0) {
            currentTarget.sendMessage(Component.text("남은 기회: " + remaining + "번", NamedTextColor.YELLOW));
        } else {
            currentTarget.sendMessage(Component.text("모든 기회를 소진하여 게임에서 탈락했습니다.", NamedTextColor.RED));
            game.broadCast(Component.text(currentTarget.getName() + " 님이 탈락했습니다.", NamedTextColor.GRAY), game.getActivePlayers());
            game.eliminatePlayer(currentTarget);
        }

        cleanupJudgementMode();
        nextTurn();
    }

    @Override
    public void onVoteStatusUpdate(String message) {
        game.broadCast(Component.text(message, NamedTextColor.YELLOW), game.getActivePlayers());
    }
}