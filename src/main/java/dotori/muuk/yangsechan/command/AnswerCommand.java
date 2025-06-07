package dotori.muuk.yangsechan.command;

import dotori.muuk.yangsechan.Main;
import dotori.muuk.yangsechan.main.Game;
import dotori.muuk.yangsechan.main.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AnswerCommand implements CommandExecutor {
    GameManager gameManager;
    public AnswerCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("대체 네가 이걸 왜 쓰는거에요");
            return true;
        }
        Game game = gameManager.getGameByPlayer(player);
        if (game == null) {
            sender.sendMessage("게임에 참여하고 있지 않습니다!");
            return true;
        }
        game.handleAnswer(player);
        return true;
    }
}
