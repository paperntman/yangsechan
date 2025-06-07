package dotori.muuk.yangsechan.main.phase;

import dotori.muuk.yangsechan.util.NodEvent;
import dotori.muuk.yangsechan.util.ShakeEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;

public interface PhaseLogic {

    void onEnter();
    void onExit();

    void onChat(AsyncChatEvent event);
    void onNod(NodEvent event);
    void onShake(ShakeEvent event);
    void onAnswer(Player player);
}