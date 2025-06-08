package dotori.muuk.yangsechan.main.phase;

import dotori.muuk.yangsechan.Main;
import dotori.muuk.yangsechan.main.Game;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import dotori.muuk.yangsechan.util.NodEvent;
import dotori.muuk.yangsechan.util.ShakeEvent;

import java.util.List;
import java.util.stream.Collectors;

public class EndPhaseLogic implements PhaseLogic {

    private final Game game;

    public EndPhaseLogic(Game game) {
        this.game = game;
    }

    @Override
    public void onEnter() {
        // --- 최종 결과 발표 로직 강화 ---
        List<Player> allPlayers = game.getPlayers();
        List<Player> eliminated = game.getEliminatedPlayers();
        List<Player> winners = allPlayers.stream()
                .filter(p -> !eliminated.contains(p))
                .toList();

        String winnerNames = winners.stream().map(Player::getName).collect(Collectors.joining(", "));
        String loserNames = eliminated.stream().map(Player::getName).collect(Collectors.joining(", "));

        // --- 1. 승/패자 발표 메시지 ---
        Component summaryMessage = Component.text()
                .append(Component.text(">> 양세찬 게임 종료! <<\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("승자: ", NamedTextColor.GREEN)).append(Component.text(winnerNames.isEmpty() ? "없음" : winnerNames, NamedTextColor.WHITE))
                .append(Component.text("\n패자: ", NamedTextColor.RED)).append(Component.text(loserNames.isEmpty() ? "없음" : loserNames, NamedTextColor.WHITE))
                .build();

        game.broadCast(summaryMessage);

        // --- 2. 각 플레이어의 단어 공개 메시지 (새로 추가된 부분) ---
        TextComponent.Builder wordListMessageBuilder = Component.text()
                .append(Component.text("\n--- 최종 단어 목록 ---\n", NamedTextColor.YELLOW));

        // getPlayerWordMap()을 사용하여 단어 목록을 만듭니다.
        game.getPlayerWordMap().forEach((uuid, word) -> {
            Player player = Bukkit.getPlayer(uuid);
            String playerName = (player != null) ? player.getName() : "오프라인";

            wordListMessageBuilder.append(Component.text(playerName, NamedTextColor.WHITE))
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(word, NamedTextColor.AQUA))
                    .append(Component.newline());
        });

        game.broadCast(wordListMessageBuilder.build());


        // --- 3. 모든 플레이어의 UI 정리 ---
        game.getPlayers().forEach(p -> {
            p.resetTitle();
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        });

        // --- 4. 5초 후에 게임을 완전히 제거하도록 예약 ---
        Bukkit.getScheduler().runTaskLater(Main.plugin, () -> game.getGameManager().removeGame(game), 100L); // 5초 = 100틱
    }

    @Override
    public void onExit() {
        // 이 페이즈는 게임의 마지막이므로 onExit이 호출될 일이 거의 없음
    }

    @Override
    public void onChat(AsyncChatEvent event) {}

    @Override
    public void onNod(NodEvent event) {}

    @Override
    public void onShake(ShakeEvent event) {}

    @Override
    public void onAnswer(Player player) {}
}