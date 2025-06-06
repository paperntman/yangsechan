package dotori.muuk.yangsechan;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HeadMovementListener implements Listener {

    private final JavaPlugin plugin;

    // 스레드 안전성을 위해 ConcurrentHashMap 사용
    private final Map<UUID, LinkedList<Float>> playerPitchHistory = new ConcurrentHashMap<>();
    private final Map<UUID, LinkedList<Float>> playerYawHistory = new ConcurrentHashMap<>();
    
    // 분석이 너무 자주 실행되지 않도록 플레이어별 쿨다운 관리
    private final Set<UUID> analysisCooldown = new HashSet<>();

    public HeadMovementListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // 시야만 변경된 경우에만 처리
        if (event.getFrom().getPitch() == event.getTo().getPitch() && event.getFrom().getYaw() == event.getTo().getYaw()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        // --- 데이터 수집 (Sliding Window) ---
        // Pitch 데이터 추가
        LinkedList<Float> pitches = playerPitchHistory.computeIfAbsent(playerUUID, k -> new LinkedList<>());
        pitches.add(event.getTo().getPitch());
        while (pitches.size() > HeadMovementAnalyzer.ANALYSIS_WINDOW_SIZE) {
            pitches.removeFirst(); // 윈도우 크기 유지
        }

        // Yaw 데이터 추가
        LinkedList<Float> yaws = playerYawHistory.computeIfAbsent(playerUUID, k -> new LinkedList<>());
        yaws.add(event.getTo().getYaw());
        while (yaws.size() > HeadMovementAnalyzer.ANALYSIS_WINDOW_SIZE) {
            yaws.removeFirst(); // 윈도우 크기 유지
        }

        // --- 분석 요청 ---
        // 쿨다운 중이 아니고, 데이터가 충분히 쌓였을 때 분석 시도
        if (!analysisCooldown.contains(playerUUID) && pitches.size() >= HeadMovementAnalyzer.ANALYSIS_WINDOW_SIZE) {
            runAnalysis(player);
        }
    }
    
    private void runAnalysis(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // 쿨다운 시작
        analysisCooldown.add(playerUUID);

        // 분석 로직 실행 (비동기로 처리하여 메인 스레드 부하 감소 고려 가능, 여기선 간단히 동기 처리)
        List<Float> pitchData = new ArrayList<>(playerPitchHistory.get(playerUUID));
        List<Float> yawData = new ArrayList<>(playerYawHistory.get(playerUUID));

        boolean isNodding = HeadMovementAnalyzer.isContinuousNod(pitchData);
        if (isNodding) {
            // 끄덕임 이벤트 호출!
            Bukkit.getPluginManager().callEvent(new NodEvent(player));
            
            // 이벤트 발생 후 데이터 초기화 및 쿨다운 설정
            playerPitchHistory.get(playerUUID).clear();
            playerYawHistory.get(playerUUID).clear(); // 다른 동작과 겹치지 않도록 함께 초기화
            setCooldown(playerUUID, 20); // 1초 쿨다운
            return; // 젓기 분석은 건너뜀
        }
        
        boolean isShaking = HeadMovementAnalyzer.isContinuousShake(yawData);
        if (isShaking) {
            // 젓기 이벤트 호출!
            Bukkit.getPluginManager().callEvent(new ShakeEvent(player));

            // 이벤트 발생 후 데이터 초기화 및 쿨다운 설정
            playerPitchHistory.get(playerUUID).clear();
            playerYawHistory.get(playerUUID).clear();
            setCooldown(playerUUID, 20); // 1초 쿨다운
            return;
        }

        // 아무 동작도 감지되지 않으면 짧은 쿨다운만 적용
        // (분석이 너무 자주 실행되는 것을 방지)
        setCooldown(playerUUID, 10); // 0.5초(10틱) 후에 다시 분석 가능
    }
    
    private void setCooldown(UUID uuid, int ticks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                analysisCooldown.remove(uuid);
            }
        }.runTaskLater(plugin, ticks);
    }
}