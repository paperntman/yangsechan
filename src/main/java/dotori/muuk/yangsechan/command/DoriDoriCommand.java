package dotori.muuk.yangsechan.command;

import dotori.muuk.yangsechan.util.NodEvent;
import dotori.muuk.yangsechan.util.ShakeEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class DoriDoriCommand implements CommandExecutor, Listener {
    private static boolean broadcast = false;
    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        broadcast = !broadcast;
        sender.sendMessage(broadcast+"");
        return true;
    }

    @EventHandler
    public void onNod(NodEvent event){
        if(broadcast)
            event.getPlayer().getServer().broadcast(Component.text(event.getPlayer().getName()+" 끄덕끄덕"));
    }
    @EventHandler
    public void onShake(ShakeEvent event){
        if(broadcast)
            event.getPlayer().getServer().broadcast(Component.text(event.getPlayer().getName()+" 도리도리"));
    }
}
