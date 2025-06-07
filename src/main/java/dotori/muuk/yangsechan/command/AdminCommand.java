package dotori.muuk.yangsechan.command;

import dotori.muuk.yangsechan.main.Game;
import dotori.muuk.yangsechan.main.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final GameManager gameManager;
    private final Component prefix = Component.text("[양세찬 어드민] ", NamedTextColor.RED);

    public AdminCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("yangsechan.admin")) {
            sender.sendMessage(prefix.append(Component.text("이 명령어를 사용할 권한이 없습니다.", NamedTextColor.GRAY)));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "list" -> handleListCommand(sender);
            case "info" -> handleInfoCommand(sender, args);
            case "stop" -> handleStopCommand(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleListCommand(CommandSender sender) {
        Collection<Game> games = gameManager.getAllGames();
        if (games.isEmpty()) {
            sender.sendMessage(prefix.append(Component.text("현재 진행 중인 게임이 없습니다.", NamedTextColor.GRAY)));
            return;
        }

        sender.sendMessage(prefix.append(Component.text("--- 진행 중인 게임 목록 ---", NamedTextColor.YELLOW)));
        for (Game game : games) {
            String gameIdStr = game.getGameId().toString();
            Component message = Component.text(" ● ", NamedTextColor.GRAY)
                    .append(Component.text("ID: " + gameIdStr.substring(0, 8) + "...", NamedTextColor.WHITE))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Phase: " + game.getCurrentPhase(), NamedTextColor.AQUA))
                    .hoverEvent(HoverEvent.showText(Component.text("클릭하여 상세 정보 보기\nID: " + gameIdStr)))
                    .clickEvent(ClickEvent.runCommand("/yadmin info " + gameIdStr));
            sender.sendMessage(message);
        }
    }

    private void handleInfoCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(prefix.append(Component.text("게임 ID를 입력해주세요. 사용법: /yadmin info <게임ID>", NamedTextColor.GRAY)));
            return;
        }

        try {
            UUID gameId = UUID.fromString(args[1]);
            Game game = gameManager.getGameById(gameId);
            if (game == null) {
                sender.sendMessage(prefix.append(Component.text("해당 ID의 게임을 찾을 수 없습니다.", NamedTextColor.GRAY)));
                return;
            }

            sender.sendMessage(prefix.append(Component.text("--- 게임 상세 정보 (" + game.getGameId().toString().substring(0, 8) + ") ---", NamedTextColor.YELLOW)));
            sender.sendMessage(Component.text(" - Phase: ", NamedTextColor.AQUA).append(Component.text(game.getCurrentPhase().toString(), NamedTextColor.WHITE)));

            sender.sendMessage(Component.text(" - 플레이어 및 단어:", NamedTextColor.AQUA));
            game.getPlayerWordMap().forEach((uuid, word) -> {
                Player p = Bukkit.getPlayer(uuid);
                String playerName = (p != null) ? p.getName() : "오프라인";
                sender.sendMessage(Component.text("   - " + playerName + ": ", NamedTextColor.GRAY).append(Component.text(word, NamedTextColor.WHITE)));
            });
            if(game.getPlayerWordMap().isEmpty()){
                game.getPlayers().forEach(p -> {
                    sender.sendMessage(Component.text("   - " + p.getName() + ": ", NamedTextColor.GRAY).append(Component.text("(단어 미정)", NamedTextColor.DARK_GRAY)));
                });
            }

        } catch (IllegalArgumentException e) {
            sender.sendMessage(prefix.append(Component.text("유효하지 않은 게임 ID 형식입니다.", NamedTextColor.GRAY)));
        }
    }

    private void handleStopCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(prefix.append(Component.text("게임 ID를 입력해주세요. 사용법: /yadmin stop <게임ID>", NamedTextColor.GRAY)));
            return;
        }

        try {
            UUID gameId = UUID.fromString(args[1]);
            Game game = gameManager.getGameById(gameId);
            if (game == null) {
                sender.sendMessage(prefix.append(Component.text("해당 ID의 게임을 찾을 수 없습니다.", NamedTextColor.GRAY)));
                return;
            }

            game.forceStop(sender.getName());
            sender.sendMessage(prefix.append(Component.text(gameId.toString().substring(0, 8) + "... 게임을 강제 종료했습니다.", NamedTextColor.YELLOW)));

        } catch (IllegalArgumentException e) {
            sender.sendMessage(prefix.append(Component.text("유효하지 않은 게임 ID 형식입니다.", NamedTextColor.GRAY)));
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(prefix.append(Component.text("--- 명령어 사용법 ---", NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("/yadmin list", NamedTextColor.WHITE).append(Component.text(" - 모든 게임 목록을 봅니다.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/yadmin info <ID>", NamedTextColor.WHITE).append(Component.text(" - 특정 게임의 상세 정보를 봅니다.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/yadmin stop <ID>", NamedTextColor.WHITE).append(Component.text(" - 특정 게임을 강제 종료합니다.", NamedTextColor.GRAY)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("yangsechan.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return List.of("list", "info", "stop").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("stop"))) {
            return gameManager.getAllGames().stream()
                    .map(game -> game.getGameId().toString())
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}