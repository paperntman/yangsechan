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
import java.util.stream.Collectors;

public class Game {
    private final UUID gameId;
    private GamePhase currentPhase; // 현재 게임 페이즈 (Enum으로 관리)
    private final Player owner; // 게임을 시작한 사람
    private final GameManager gameManager;

    // --- 게임 데이터 ---
    private final List<Player> players;
    private final Map<UUID, String> playerWordMap;

    // --- '패배' 및 '기회' 관련 데이터 추가 ---
    private final Map<UUID, Integer> playerAttemptCount;
    private final List<Player> eliminatedPlayers;
    private static final int MAX_ATTEMPTS = 3;

    // Phase별 로직 처리기 (State 패턴)
    private PhaseLogic phaseLogic;

    public Game(Player owner, GameManager manager) {
        this.gameId = UUID.randomUUID();
        this.owner = owner;
        this.players = new ArrayList<>();
        this.playerWordMap = new HashMap<>();
        this.playerAttemptCount = new HashMap<>(); // 시도 횟수 맵 초기화
        this.eliminatedPlayers = new ArrayList<>(); // 탈락자 리스트 초기화
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
        playerAttemptCount.remove(player.getUniqueId());
        eliminatedPlayers.remove(player);
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

    public void broadCastTitle(@NotNull TextComponent title, @NotNull TextComponent subtitle, Collection<Player> players) {
        for (Player player : players) {
            player.showTitle(Title.title(title, subtitle));
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
        if (phaseLogic != null) {
            phaseLogic.onExit();
        }
        Component stopMessage = Component.text()
                .append(Component.text("🚨 게임이 관리자(" + adminName + ")에 의해 강제 종료되었습니다.", NamedTextColor.RED))
                .build();
        broadCast(stopMessage);
        gameManager.removeGame(this);
    }

    public Map<UUID, String> getPlayerWordMap() {
        return playerWordMap;
    }

    // --- '패배' 및 '기회' 관련 새 메서드 ---

    /**
     * 게임에 참여한 모든 플레이어의 시도 횟수를 최댓값으로 초기화합니다.
     */
    public void initializeAttempts() {
        for (Player player : players) {
            playerAttemptCount.put(player.getUniqueId(), MAX_ATTEMPTS);
        }
    }

    /**
     * 특정 플레이어의 남은 시도 횟수를 반환합니다.
     */
    public int getRemainingAttempts(Player player) {
        return playerAttemptCount.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * 특정 플레이어의 시도 횟수를 1 감소시키고, 감소 후 남은 횟수를 반환합니다.
     */
    public int decrementAndGetAttempts(Player player) {
        int remaining = getRemainingAttempts(player) - 1;
        if (remaining < 0) remaining = 0;
        playerAttemptCount.put(player.getUniqueId(), remaining);
        return remaining;
    }

    /**
     * 특정 플레이어를 탈락 처리합니다.
     */
    public void eliminatePlayer(Player player) {
        if (!eliminatedPlayers.contains(player)) {
            eliminatedPlayers.add(player);
        }
    }

    /**
     * 특정 플레이어가 탈락했는지 확인합니다.
     */
    public boolean isEliminated(Player player) {
        return eliminatedPlayers.contains(player);
    }

    /**
     * 현재 게임에 '활동 중인' (탈락하지 않고, 성공하지도 않은) 플레이어 목록을 반환합니다.
     */
    public List<Player> getActivePlayers() {
        return players.stream()
                .filter(p -> !eliminatedPlayers.contains(p))
                .collect(Collectors.toList());
    }

    public List<Player> getEliminatedPlayers() {
        return eliminatedPlayers;
    }
}