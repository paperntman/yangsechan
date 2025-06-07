package dotori.muuk.yangsechan.command;

import dotori.muuk.yangsechan.Main;
import dotori.muuk.yangsechan.main.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Starter implements CommandExecutor {
    GameManager gameManager;
    public Starter(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (commandSender instanceof Player player)
            gameManager.createGame(player);
        return true;
    }
}
