package dotori.muuk.yangsechan;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Starter implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (GameManager.isGameStarted()) {
            commandSender.sendMessage(Component.text("게임이 이미 시작되었습니다!", NamedTextColor.RED));
            return true;
        }
        GameManager.StartGame(commandSender);
        return true;
    }
}
