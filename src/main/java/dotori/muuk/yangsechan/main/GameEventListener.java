package dotori.muuk.yangsechan.main;

import dotori.muuk.yangsechan.util.NodEvent;
import dotori.muuk.yangsechan.util.ShakeEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class GameEventListener implements Listener {

    private final GameManager gameManager;
    
    public GameEventListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        for (Game game : gameManager.getAllGames()) {
            game.handleChat(event);
        }
    }

    @EventHandler
    public void onPlayerNod(NodEvent event) {
        for (Game game : gameManager.getAllGames()) {
            game.handleNod(event);
        }
    }

    @EventHandler
    public void onPlayerShake(ShakeEvent event) {
        for (Game game : gameManager.getAllGames()) {
            game.handleShake(event);
        }
    }


}