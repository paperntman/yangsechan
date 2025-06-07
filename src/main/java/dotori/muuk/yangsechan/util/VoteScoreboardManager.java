package dotori.muuk.yangsechan.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin; // JavaPlugin 임포트
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class VoteScoreboardManager {

    // 플러그인 메인 인스턴스를 저장할 필드 추가
    private final JavaPlugin plugin;

    private Scoreboard scoreboard;
    private Objective objective;
    private final Map<UUID, Team> playerTeams = new HashMap<>();

    /**
     * 스코어보드 매니저 생성자
     * @param plugin 플러그인의 메인 클래스 인스턴스
     */
    public VoteScoreboardManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 지정된 플레이어들에게 투표 스코어보드를 생성하고 보여줍니다.
     * @param players 스코어보드를 볼 모든 플레이어 리스트
     */
    public void createScoreboard(List<Player> players) {
        // 스케줄러를 사용해 메인 스레드에서 코드를 실행하도록 변경
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            // null 체크를 manager에 대해 수행

            this.scoreboard = manager.getNewScoreboard();

            Component objectiveTitle = Component.text("투표 현황")
                    .color(NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD);

            this.objective = scoreboard.registerNewObjective("voteStatus", Criteria.DUMMY, objectiveTitle);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            for (int i = 0; i < players.size(); i++) {
                Player p = players.get(i);
                Team team = scoreboard.registerNewTeam("player_" + i);
                team.addEntry(p.getName());
                playerTeams.put(p.getUniqueId(), team);
                objective.getScore(p.getName()).setScore(players.size() - i);
            }

            players.forEach(p -> p.setScoreboard(scoreboard));
        });
    }

    /**
     * 플레이어들의 투표 상태가 변경될 때 스코어보드를 업데이트합니다.
     * @param statuses 플레이어 UUID와 현재 투표 상태(NodStatus)가 담긴 Map
     */
    public void updateScoreboard(Map<UUID, NodStatus> statuses) {
        // 스케줄러를 사용해 메인 스레드에서 코드를 실행하도록 변경
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (scoreboard == null) return;

            statuses.forEach((uuid, status) -> {
                Team team = playerTeams.get(uuid);
                if (team != null) {
                    team.suffix(getStatusComponent(status));
                }
            });
        });
    }

    /**
     * 게임 단계가 끝났을 때 스코어보드를 제거합니다.
     */
    public void cleanup() {
        // 스케줄러를 사용해 메인 스레드에서 코드를 실행하도록 변경
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (scoreboard != null) {
                playerTeams.keySet().stream()
                        .map(Bukkit::getPlayer)
                        .filter(Objects::nonNull)
                        .forEach(p -> p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()));
            }
            playerTeams.clear();
            scoreboard = null;
            objective = null;
        });
    }

    private Component getStatusComponent(NodStatus status) {
        return switch (status) {
            case NOD -> Component.text(" [")
                    .append(Component.text("O", NamedTextColor.GREEN))
                    .append(Component.text("]"));

            case SHAKE -> Component.text(" [")
                    .append(Component.text("X", NamedTextColor.RED))
                    .append(Component.text("]"));

            case NORMAL -> Component.text(" [-]", NamedTextColor.GRAY);
        };
    }
}