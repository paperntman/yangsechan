package dotori.muuk.yangsechan.main.phase;

import dotori.muuk.yangsechan.main.Game;
import dotori.muuk.yangsechan.util.NodEvent;
import dotori.muuk.yangsechan.util.ShakeEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RecruitingPhaseLogic implements PhaseLogic {
    Game game;
    public RecruitingPhaseLogic(Game game) {
        this.game = game;
    }

    @Override
    public void onEnter() {
        game.getGameManager().getPlugin().getServer().broadcast(Component.text(game.getOwner().getName() + " 플레이어가 주최하는 양세찬게임에 참여하시려면 고개를 끄덕여 주세요 앞으로 10초 간 참가 가능합니다!"));
        Bukkit.getScheduler().runTaskLater(game.getGameManager().getPlugin(), () -> game.setPhase(GamePhase.WORD_SELECTION), 200);
    }

    @Override
    public void onExit() {
        game.getGameManager().getPlugin().getServer().broadcast(Component.text(game.getOwner().getName() + " 플레이어가 주최하는 양세찬게임의 모집이 끝났습니다 !"));
    }

    @Override
    public void onChat(AsyncChatEvent event) {

    }

    @Override
    public void onNod(NodEvent event) {
        System.out.println(event.getPlayer());
        if (game.getCurrentPhase() !=  GamePhase.RECRUITING) return;
        Player player = event.getPlayer();
        Game gameJoined = game.getGameManager().getGameByPlayer(player);
        if (gameJoined != null) {
            return;
        }
        game.getGameManager().addPlayerToGame(player, game);
        player.sendMessage(game.getOwner().getName() + " 플레이어가 주최하는 양세찬게임에 참여했습니다! 방을 나가시려면, 고개를 저어 주세요!");
    }

    @Override
    public void onShake(ShakeEvent event) {
        Player player = event.getPlayer();
        Game gameJoined = game.getGameManager().getGameByPlayer(player);
        if (game.equals(gameJoined)) {
            player.sendMessage("게임에서 나갔습니다!");
            game.getGameManager().removePlayerFromGame(player, game);
        }
    }

    @Override
    public void onAnswer(Player player) {

    }
}
