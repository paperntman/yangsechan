package dotori.muuk.yangsechan.util;

import dotori.muuk.yangsechan.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoteManager {

    private final VoteResultListener resultListener; // 투표 결과를 통보할 리스너
    private final Collection<Player> voters; // 투표권을 가진 플레이어들
    private final Map<UUID, NodStatus> voteStatus; // 플레이어별 투표 상태

    private BukkitTask confirmationTask; // 5초 카운트다운 작업을 위한 Task
    private boolean isVoteActive = true;

    /**
     * 새로운 투표 세션을 생성합니다.
     * @param voters 투표에 참여할 플레이어 목록
     * @param resultListener 투표 결과를 전달받을 콜백 리스너
     */
    public VoteManager(Collection<Player> voters, VoteResultListener resultListener) {
        this.voters = voters;
        this.resultListener = resultListener;
        this.voteStatus = new ConcurrentHashMap<>();
        
        // 모든 투표자를 '무응답' 상태로 초기화
        for (Player voter : voters) {
            voteStatus.put(voter.getUniqueId(), NodStatus.NORMAL);
        }
    }

    /**
     * 특정 플레이어의 투표를 기록합니다.
     * @param player 투표한 플레이어
     * @param status 새로운 투표 상태 (NOD 또는 SHAKE)
     */
    public void castVote(Player player, NodStatus status) {
        if (!isVoteActive || !voters.contains(player) || voteStatus.get(player.getUniqueId()) == status) {
            return; // 투표가 비활성이거나, 투표권이 없거나, 이미 같은 표를 던졌으면 무시
        }
        
        voteStatus.put(player.getUniqueId(), status);
        checkVoteStatus();
    }
    
    /**
     * 현재 투표 현황을 판정 로직에 따라 검사합니다.
     */
    private void checkVoteStatus() {
        if (!isVoteActive) return;

        long totalVoters = voters.size();
        long nodCount = voteStatus.values().stream().filter(s -> s == NodStatus.NOD).count();
        long shakeCount = voteStatus.values().stream().filter(s -> s == NodStatus.SHAKE).count();

        // 판정 로직 1: 기각 (반대가 과반수 초과)
        if (shakeCount > totalVoters / 2) {
            cleanup();
            resultListener.onVoteRejected("과반수 반대로");
            return;
        }

        // 판정 로직 2: 만장일치 확정
        if (nodCount == totalVoters) {
            cleanup();
            resultListener.onVoteConfirmed("만장일치로");
            return;
        }

        // 판정 로직 3: 자동 확정 타이머
        boolean shouldStartTimer = (nodCount > totalVoters / 2) && (shakeCount == 0);

        if (shouldStartTimer) {
            // 아직 타이머가 없다면 새로 시작
            if (confirmationTask == null) {
                resultListener.onVoteStatusUpdate("5초 동안 반대가 없으면 확정됩니다!");
                confirmationTask = Bukkit.getScheduler().runTaskLater(Main.plugin, () -> {
                    cleanup();
                    resultListener.onVoteConfirmed("과반수 찬성으로");
                }, 100L); // 5초 = 100틱
            }
        } else {
            // 타이머가 작동 중이었는데, 반대표가 나왔거나 찬성표가 과반수 미만이 된 경우
            if (confirmationTask != null) {
                confirmationTask.cancel();
                confirmationTask = null;
                resultListener.onVoteStatusUpdate("반대표가 있어 자동 확정이 중단됩니다.");
            }
        }
    }
    
    /**
     * 투표 세션을 강제로 종료하고 모든 리소스를 정리합니다.
     */
    public void cleanup() {
        if (!isVoteActive) return;
        this.isVoteActive = false;
        if (confirmationTask != null && !confirmationTask.isCancelled()) {
            confirmationTask.cancel();
        }
        voteStatus.clear();
    }
    
    // 현재 투표 현황을 외부에서 조회할 수 있도록 Getter 제공
    public Map<UUID, NodStatus> getVoteStatus() {
        return voteStatus;
    }

    public Collection<Player> getVoters() {
        return voters;
    }
}