package dotori.muuk.yangsechan.main;

import dotori.muuk.yangsechan.discord.DiscordManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final JavaPlugin plugin;
    private final DiscordManager discordManager;

    public GameManager(JavaPlugin plugin, DiscordManager discordManager) {
        this.plugin = plugin;
        this.discordManager = discordManager;
    }

    private final Map<UUID, Game> games = new HashMap<>();
    private final Map<UUID, Game> playerGameMap = new HashMap<>();

    public void createGame(Player owner) {
        if (playerGameMap.containsKey(owner.getUniqueId())) {
            owner.sendMessage("이미 다른 게임에 참여중입니다.");
            return;
        }
        Game newGame = new Game(owner, this);
        games.put(newGame.getGameId(), newGame);
        playerGameMap.put(owner.getUniqueId(), newGame);
    }

    @Nullable
    public Game getGameByPlayer(Player player) {
        return playerGameMap.get(player.getUniqueId());
    }

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
        game.removePlayer(player); // Game 객체 내부에서도 플레이어 데이터를 모두 정리
    }

    public void removeGame(Game game) {
        games.remove(game.getGameId());
        for (Player p : game.getPlayers()) {
            playerGameMap.remove(p.getUniqueId());
        }
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public Collection<Game> getAllGames() {
        return games.values();
    }

    public Game getGameById(UUID gameId) {
        return games.get(gameId);
    }
}