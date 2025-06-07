package dotori.muuk.yangsechan.main.phase;

import dotori.muuk.yangsechan.Main;
import dotori.muuk.yangsechan.main.Game;
import dotori.muuk.yangsechan.main.GameManager;
import dotori.muuk.yangsechan.util.NodEvent;
import dotori.muuk.yangsechan.util.ShakeEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

// 게임의 마지막을 장식하고 안전하게 정리하는 역할
public class EndPhaseLogic implements PhaseLogic {

    private final Game game;

    public EndPhaseLogic(Game game) {
        this.game = game;
    }

    @Override
    public void onEnter() {
        // 1. 마지막 인사 및 결과 발표
        Component endMessage = Component.text()
                .append(Component.text("양세찬 게임 종료!\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("모두 수고하셨습니다!", NamedTextColor.YELLOW))
                .build();
        game.broadCast(endMessage);

        // 2. 모든 플레이어의 UI 정리 (스코어보드, 타이틀 등)
        game.getPlayers().forEach(p -> {
            p.resetTitle();
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        });

        // 3. 5초 후에 게임을 완전히 제거하도록 예약 (플레이어가 메시지를 읽을 시간 제공)
        Bukkit.getScheduler().runTaskLater(Main.plugin, () -> {
            game.getGameManager().removeGame(game);
        }, 100L); // 5초 = 100틱
    }

    @Override
    public void onExit() {
        // 이 페이즈는 게임의 마지막이므로 onExit이 호출될 일이 거의 없음
    }

    // --- 이하 이벤트 핸들러들은 비워둠 ---
    @Override
    public void onChat(AsyncChatEvent event) {}

    @Override
    public void onNod(NodEvent event) {

    }

    @Override
    public void onShake(ShakeEvent event) {

    }

    @Override
    public void onAnswer(Player player) {}
}