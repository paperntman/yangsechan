package dotori.muuk.yangsechan.main;

import dotori.muuk.yangsechan.main.phase.*;
import dotori.muuk.yangsechan.util.NodEvent;
import dotori.muuk.yangsechan.util.ShakeEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Game {
    private final UUID gameId;
    private GamePhase currentPhase; // 현재 게임 페이즈 (Enum으로 관리)
    private final Player owner; // 게임을 시작한 사람
    private final GameManager gameManager;

    // 게임 데이터
    private final List<Player> players;
    private final Map<UUID, String> playerWordMap;

    // Phase별 로직 처리기 (State 패턴)
    private PhaseLogic phaseLogic;

    public Game(Player owner, GameManager manager) {
        this.gameId = UUID.randomUUID();
        this.owner = owner;
        this.players = new ArrayList<>();
        this.playerWordMap = new HashMap<>();
        this.gameManager = manager;

        // 초기 단계는 '모집' 단계로 설정
        setPhase(GamePhase.RECRUITING);
        addPlayer(owner);
    }

    private PhaseLogic createLogicForPhase(GamePhase phase) {
        return switch (phase) {
            case RECRUITING -> new RecruitingPhaseLogic(this);
            case WORD_SELECTION -> new WordSelectionPhaseLogic(this);
            case MAIN_LOOP -> new MainLoopPhaseLogic(this);
            case ENDED -> new EndPhaseLogic(this);
        };
    }

    public void setPhase(GamePhase newPhase) {
        if (currentPhase == newPhase) return;

        if (phaseLogic != null) {
            phaseLogic.onExit();
        }

        this.currentPhase = newPhase;
        this.phaseLogic = createLogicForPhase(newPhase);

        this.phaseLogic.onEnter();
    }

    public void addPlayer(Player player) {
        players.add(player);
    }


    public void removePlayer(Player player) {
        players.remove(player);
        playerWordMap.remove(player.getUniqueId());
    }

    public void handleChat(AsyncChatEvent event) {
        if (phaseLogic != null) {
            phaseLogic.onChat(event);
        }
    }
    public void handleNod(NodEvent event) {
        if (phaseLogic != null) {
            phaseLogic.onNod(event);
        }
    }
    public void handleShake(ShakeEvent event) {
        if (phaseLogic != null) {
            phaseLogic.onShake(event);
        }
    }
    public void handleAnswer(Player player) {
        if (phaseLogic != null) {
            phaseLogic.onAnswer(player);
        }
    }


    public UUID getGameId() {
        return gameId;
    }

    public List<Player> getPlayers() {
        return players;
    }
    public void broadCast(Component component) {
        players.forEach(player -> player.sendMessage(component));
    }


    public void broadCast(Component component, Collection<Player> players) {
        players.forEach(player -> player.sendMessage(component));
    }

    public Player getOwner() {
        return owner;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public void setPlayerWord(Player target, String proposedWord) {
        playerWordMap.put(target.getUniqueId(), proposedWord);
    }

    public void broadCastTitle(@NotNull TextComponent title, @NotNull TextComponent subtitle, Collection<Player> voters) {
        for (Player voter : voters) {
            voter.showTitle(Title.title(
                    title, subtitle
            ));
        }
    }

    public String getPlayerWord(Player currentTarget) {
        return playerWordMap.get(currentTarget.getUniqueId());
    }

    public void broadCastTitle(@NotNull Title title, List<Player> others) {
        for (Player other : others) {
            other.showTitle(title);
        }
    }

    public void forceStop(String adminName) {
        // 1. 현재 페이즈의 종료 로직을 호출하여 UI 등을 정리
        if (phaseLogic != null) {
            phaseLogic.onExit();
        }

        // 2. 모든 플레이어에게 강제 종료 알림
        Component stopMessage = Component.text()
                .append(Component.text("🚨 게임이 관리자(" + adminName + ")에 의해 강제 종료되었습니다.", NamedTextColor.RED))
                .build();
        broadCast(stopMessage);

        // 3. GameManager에서 이 게임을 제거
        gameManager.removeGame(this);
    }

    public Map<UUID, String> getPlayerWordMap() {
        return playerWordMap;
    }
}

