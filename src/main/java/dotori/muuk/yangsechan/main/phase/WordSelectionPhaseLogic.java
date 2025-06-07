package dotori.muuk.yangsechan.main.phase;

import dotori.muuk.yangsechan.main.Game;
import dotori.muuk.yangsechan.util.*;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class WordSelectionPhaseLogic implements PhaseLogic, VoteResultListener {

    private final Game game;
    private VoteManager currentVote;
    private VoteScoreboardManager scoreboardManager; // 스코어보드 매니저 인스턴스 추가
    private String proposedWord;

    private Player target;
    private Queue<Player> turn;

    public WordSelectionPhaseLogic(Game game) {
        this.game = game;
    }

    @Override
    public void onEnter() {
        if (game.getPlayers().size() < 2){
            game.forceStop("admin");
            return;
        }
        turn = new LinkedList<>(game.getPlayers());
        nextPlayer();
    }

    @Override
    public void onExit() {
        game.broadCast(Component.text("단어 선정이 끝났습니다!"));
    }

    private void nextPlayer() {
        target = turn.poll();
        if (target == null) {
            game.setPhase(GamePhase.MAIN_LOOP);
            return;
        }

        target.showTitle(
                Title.title(
                        Component.text("당신의 단어가 정해지고 있습니다!"),
                        Component.text("다른 플레이어들이 토론 중입니다...", NamedTextColor.GRAY)));

        List<Player> others = new ArrayList<>(game.getPlayers());
        others.remove(target);
        game.broadCast(Component.text()
                .append(Component.text(target.getName(), NamedTextColor.GOLD))
                .append(Component.text(" 님의 단어를 정해주세요! 자유롭게 토론하고 채팅으로 단어를 제안하세요.", NamedTextColor.YELLOW))
                .build(), others);
    }

    @Override
    public void onChat(AsyncChatEvent event) {
        if (event.getPlayer() == target) return;
        event.viewers().remove(target);
        if (currentVote != null) {
            event.getPlayer().sendMessage("이미 단어 투표가 진행 중입니다.");
            return;
        }
        // ...
        if (!(event.message() instanceof TextComponent component)) return;
        this.proposedWord = component.content(); // 실제로는 TextComponent 처리 필요
        startNewVote();
    }

    private void startNewVote() {
        List<Player> voters = new ArrayList<>(game.getPlayers());
        voters.remove(target);

        // 1. VoteManager 생성
        this.currentVote = new VoteManager(voters, this);

        // 2. VoteScoreboardManager 생성 및 표시
        this.scoreboardManager = new VoteScoreboardManager(game.getGameManager().getPlugin());
        this.scoreboardManager.createScoreboard(voters);

        // 3. 초기 스코어보드 상태 업데이트 (모두 '무응답' 상태)
        this.scoreboardManager.updateScoreboard(currentVote.getVoteStatus());

        game.broadCastTitle(
                Component.text(proposedWord, NamedTextColor.AQUA),
                Component.text("이 단어에 대해 투표해주세요!"),
                voters
        );
    }

    @Override
    public void onNod(NodEvent event) {
        if (currentVote != null) {
            // 1. VoteManager에 투표 전달
            currentVote.castVote(event.getPlayer(), NodStatus.NOD);

            // 2. 변경된 상태로 스코어보드 업데이트
            if (scoreboardManager != null) {
                scoreboardManager.updateScoreboard(currentVote.getVoteStatus());
            }
        }
    }

    @Override
    public void onShake(ShakeEvent event) {
        if (currentVote != null) {
            // 1. VoteManager에 투표 전달
            currentVote.castVote(event.getPlayer(), NodStatus.SHAKE);

            // 2. 변경된 상태로 스코어보드 업데이트
            if (scoreboardManager != null) {
                scoreboardManager.updateScoreboard(currentVote.getVoteStatus());
            }
        }
    }

    @Override
    public void onAnswer(Player player) {

    }


    // --- VoteResultListener 구현부 ---
    @Override
    public void onVoteConfirmed(String reason) {

        game.setPlayerWord(target, proposedWord);
        game.broadCast(Component.text(reason + " 단어 '" + proposedWord + "' (으)로 확정되었습니다!"),
                currentVote.getVoteStatus().keySet().stream().map(Bukkit::getPlayer).toList());
        target.sendMessage(Component.text());

        cleanupVoteSession(); // 투표 관련 리소스 정리
        this.proposedWord = null;
        nextPlayer();
    }

    @Override
    public void onVoteRejected(String reason) {

        game.broadCast(Component.text(reason + " 단어 '" + proposedWord + "' (이)가 기각되었습니다. 다른 단어를 제안해주세요."),
                currentVote.getVoters());

        cleanupVoteSession(); // 투표 관련 리소스 정리
        this.proposedWord = null;
    }

    @Override
    public void onVoteStatusUpdate(String message) {
        game.broadCast(Component.text(message, NamedTextColor.YELLOW));
    }

    /**
     * 현재 진행 중인 투표 세션과 관련된 모든 리소스를 정리합니다.
     */
    private void cleanupVoteSession() {
        game.broadCastTitle(Component.empty(), Component.empty(), currentVote.getVoters());
        if (currentVote != null) {
            currentVote.cleanup(); // 타이머 등 내부 리소스 정리
            this.currentVote = null;
        }
        if (scoreboardManager != null) {
            scoreboardManager.cleanup(); // 스코어보드 제거
            this.scoreboardManager = null;
        }

    }

    // ... 기타 메소드 ...
}