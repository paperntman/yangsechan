package dotori.muuk.yangsechan.util;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ShakeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;

    public ShakeEvent(Player player) {
        this.player = player;
    }

    /**
     * 이벤트를 발생시킨 플레이어를 가져옵니다.
     * @return Player 객체
     */
    public Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}