// GameEventListener.java

package dotori.muuk.yangsechan.main;

import dotori.muuk.yangsechan.main.phase.GamePhase;
import dotori.muuk.yangsechan.util.NodEvent;
import dotori.muuk.yangsechan.util.ShakeEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class GameEventListener implements Listener {

    private final GameManager gameManager;

    public GameEventListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Game playerGame = gameManager.getGameByPlayer(player);

        if (playerGame != null) {
            // 플레이어가 이미 게임에 속해 있다면, 해당 게임에만 이벤트를 전달합니다.
            playerGame.handleChat(event);
        } else {
            // 플레이어가 게임에 속해있지 않다면, 모든 게임을 순회하며 처리합니다.
            // (예: 단어 선정 단계에서 토론하는 경우)
            // 이 프로젝트에서는 단어 선정 시 target을 제외한 모두에게 채팅이 보여야 하므로,
            // 이 부분은 기존 로직을 유지하는 것이 맞을 수 있습니다.
            // 다만, WordSelectionPhaseLogic의 onChat에서 target을 제외하므로, 이 또한 최적화 가능합니다.
            for (Game game : gameManager.getAllGames()) {
                game.handleChat(event);
            }
        }
    }

    @EventHandler
    public void onPlayerNod(NodEvent event) {
        Player player = event.getPlayer();
        Game playerGame = gameManager.getGameByPlayer(player);

        if (playerGame != null) {
            // 최적화된 경로: 플레이어가 이미 게임에 참여 중일 때
            playerGame.handleNod(event);
        } else {
            // 기능 유지 경로: 플레이어가 게임에 참여하지 않았을 때 (참가 시도)
            // 모든 게임을 순회하되, 모집 중인 게임에만 이벤트를 전달합니다.
            for (Game game : gameManager.getAllGames()) {
                if (game.getCurrentPhase() == GamePhase.RECRUITING) {
                    game.handleNod(event);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerShake(ShakeEvent event) {
        Player player = event.getPlayer();
        Game playerGame = gameManager.getGameByPlayer(player);

        if (playerGame != null) {
            // 최적화된 경로: 플레이어가 이미 게임에 참여 중일 때
            playerGame.handleShake(event);
        }
        // 플레이어가 게임에 참여하고 있지 않을 때의 'Shake'는 무시됩니다.
        // (RecruitingPhaseLogic에서 게임에 참여한 사람만 나갈 수 있도록 처리)
    }
}