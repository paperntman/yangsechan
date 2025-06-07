package dotori.muuk.yangsechan.main;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final JavaPlugin plugin;

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // 여러 게임 인스턴스를 관리
    private final Map<UUID, Game> games = new HashMap<>();

    // 어떤 플레이어가 어떤 게임에 속해있는지 빠르게 찾기 위한 맵
    private final Map<UUID, Game> playerGameMap = new HashMap<>();

    // 게임 생성
    public void createGame(Player owner) {
        if (playerGameMap.containsKey(owner.getUniqueId())) {
            owner.sendMessage("이미 다른 게임에 참여중입니다.");
            return;
        }
        Game newGame = new Game(owner, this);
        games.put(newGame.getGameId(), newGame);
        playerGameMap.put(owner.getUniqueId(), newGame);
    }

    // 플레이어가 속한 게임 인스턴스를 반환
    public @Nullable Game getGameByPlayer(Player player) {
        return playerGameMap.get(player.getUniqueId());
    }

    // 플레이어를 게임에 참가시킴
    public void addPlayerToGame(Player player, Game game) {
        if (playerGameMap.containsKey(player.getUniqueId())) {
            player.sendMessage("이미 다른 게임에 참여중입니다.");
            return;
        }
        game.addPlayer(player);
        playerGameMap.put(player.getUniqueId(), game);
    }

    public void removePlayerFromGame(Player player, Game game) {
        if (!playerGameMap.containsKey(player.getUniqueId())) {
            player.sendMessage("게임에 참여하고 있지 않습니다.");
            return;
        }
        playerGameMap.remove(player.getUniqueId());
        game.removePlayer(player);
    }

    // 게임 종료 처리
    public void removeGame(Game game) {
        games.remove(game.getGameId());
        for (Player p : game.getPlayers()) {
            playerGameMap.remove(p.getUniqueId());
        }
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public Collection<Game> getAllGames() {
        return games.values();
    }

    /**
     * 게임 ID로 특정 게임 인스턴스를 찾습니다.
     * @param gameId 찾을 게임의 UUID
     * @return 해당 ID의 Game 인스턴스, 없으면 null
     */
    public Game getGameById(UUID gameId) {
        return games.get(gameId);
    }
}