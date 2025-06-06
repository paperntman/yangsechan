package dotori.muuk.yangsechan;// package dotori.muuk.yangsechan;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VoteScoreboardManager {

    private Scoreboard scoreboard;
    private Objective objective;
    private final Map<UUID, Team> playerTeams = new HashMap<>();

    /**
     * 지정된 플레이어들에게 투표 스코어보드를 생성하고 보여줍니다.
     * @param players 스코어보드를 볼 모든 플레이어 리스트
     */
    public void createScoreboard(List<Player> players) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        this.scoreboard = manager.getNewScoreboard();
        
        // "투표 현황" 이라는 제목의 Objective 생성
        Component objectiveTitle = Component.text("투표 현황")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD);

        this.objective = scoreboard.registerNewObjective("voteStatus", Criteria.DUMMY, objectiveTitle);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            // 각 플레이어에 대한 팀을 만들어 접두사/접미사(투표 상태)를 관리합니다.
            Team team = scoreboard.registerNewTeam("player_" + i);
            team.addEntry(p.getName());
            playerTeams.put(p.getUniqueId(), team);

            // 초기 점수 설정 (목록 순서대로 표시)
            objective.getScore(p.getName()).setScore(players.size() - i);
        }

        // 모든 플레이어에게 이 스코어보드를 적용
        players.forEach(p -> p.setScoreboard(scoreboard));
    }

    /**
     * 플레이어들의 투표 상태가 변경될 때 스코어보드를 업데이트합니다.
     * @param statuses 플레이어 UUID와 현재 투표 상태(NodStatus)가 담긴 Map
     */
    public void updateScoreboard(Map<UUID, NodStatus> statuses) {
        if (scoreboard == null) return;

        statuses.forEach((uuid, status) -> {
            Team team = playerTeams.get(uuid);
            if (team != null) {
                team.suffix(getStatusComponent(status));
            }
        });
    }

    /**
     * 게임 단계가 끝났을 때 스코어보드를 제거합니다.
     */
    public void cleanup() {
        if (scoreboard != null) {
            // 모든 플레이어에게 기본 스코어보드로 되돌림
            playerTeams.keySet().stream()
                    .map(Bukkit::getPlayer)
                    .filter(java.util.Objects::nonNull)
                    .forEach(p -> p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()));
        }
        playerTeams.clear();
        scoreboard = null;
        objective = null;
    }

    private Component getStatusComponent(NodStatus status) {
        return switch (status) {
            case NOD -> Component.text(" [") // 1. 기본 텍스트 (흰색)
                    .append(Component.text("O", NamedTextColor.GREEN)) // 2. 초록색 'O'를 추가
                    .append(Component.text("]")); // 3. 닫는 괄호 추가 (기본 흰색)

            case SHAKE -> Component.text(" [")
                    .append(Component.text("X", NamedTextColor.RED)) // 빨간색 'X'를 추가
                    .append(Component.text("]"));

            case NORMAL -> Component.text(" [-]", NamedTextColor.GRAY); // 이 경우는 전체가 회색이므로 한 번에 처리
        };
    }
}