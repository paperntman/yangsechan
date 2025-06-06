package dotori.muuk.yangsechan;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameManager {
    private static boolean gameStarted = false;
    private static final List<Player> players = new ArrayList<>();
    private static CommandSender owner = null;
    private static Map<Player, String> playerWordMap = new HashMap<>();

    public static boolean addPlayer(Player player) {
        return players.add(player);
    }

    public static boolean isGameStarted() {
        return gameStarted;
    }

    public static void StartGame(CommandSender sender) {
        gameStarted = true;
        owner = sender;
        Bukkit.getServer().sendMessage(
                Component.text("양세찬 게임 참가자를 모집 중입니다! 게임에 참가하려면 ", NamedTextColor.YELLOW).append(
                        Component.text("[참가하기]", NamedTextColor.GREEN).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/참가")),
                        Component.text("버튼을 눌러 참가하세요!", NamedTextColor.YELLOW)
        ));
        Bukkit.getScheduler().runTaskLater(Yangsechan.plugin, () -> StartMessage(10), 20);
        Bukkit.getScheduler().runTaskLater(Yangsechan.plugin, () -> StartMessage(5), 25);
        Bukkit.getScheduler().runTaskLater(Yangsechan.plugin, () -> StartMessage(4), 26);
        Bukkit.getScheduler().runTaskLater(Yangsechan.plugin, () -> StartMessage(3), 27);
        Bukkit.getScheduler().runTaskLater(Yangsechan.plugin, () -> StartMessage(2), 28);
        Bukkit.getScheduler().runTaskLater(Yangsechan.plugin, () -> StartMessage(1), 29);
        Bukkit.getScheduler().runTaskLater(Yangsechan.plugin, GameManager::Phase_I_Start, 30);
    }

    private static void StartMessage(long time) {
        Bukkit.getServer().sendMessage(
                Component.text("양세찬 게임이 ").append(
                        Component.text(time, NamedTextColor.RED),
                        Component.text(" 초 후 시작합니다!")
                )
        );
    }

    public static boolean StopGame(CommandSender sender) {
        if (sender.isOp() || sender.equals(owner)) {
            //TODO fill
            return true;
        }
        return false;
    }

    private static void Phase_I_Start() {
        Phase_I.start(players);
    }

    public static void Phase_I_End(Map<Player, String> map){
        playerWordMap = map;
    }



}
