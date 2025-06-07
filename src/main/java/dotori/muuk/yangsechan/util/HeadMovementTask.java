package dotori.muuk.yangsechan.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * 서버 틱마다 온라인 플레이어의 머리 움직임을 추적하고 분석하는 BukkitRunnable.
 * PlayerMoveEvent 대신 서버 틱에 맞춰 동작하여 일관된 데이터 수집이 가능합니다.
 * 감지된 제스처에 따라 커스텀 이벤트(NodEvent, ShakeEvent)를 발생시킵니다.
 */
public class HeadMovementTask extends BukkitRunnable implements Listener {

    // 데이터 저장소
    private final Map<UUID, LinkedList<Float>> playerPitchHistory = new HashMap<>();
    private final Map<UUID, LinkedList<Float>> playerYawHistory = new HashMap<>();

    // 제스처 반복 감지를 막기 위한 쿨다운 관리
    private final Map<UUID, Long> gestureCooldowns = new HashMap<>();
    private static final long GESTURE_COOLDOWN_MS = 1000; // 제스처 감지 후 1초 쿨다운

    public HeadMovementTask(JavaPlugin plugin) {
        // 플레이어 접속/퇴장 이벤트를 감지하기 위해 리스너로 등록
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // 서버에 이미 접속해 있는 플레이어들을 추적 시작
        plugin.getServer().getOnlinePlayers().forEach(this::startTracking);
    }

    /**
     * 이 Task의 핵심 로직. 매 틱마다 실행됩니다.
     */
    @Override
    public void run() {
        // 현재 추적 중인 모든 플레이어에 대해 반복
        // playerPitchHistory.keySet()을 사용하면 추적 중인 플레이어만 순회하게 됨
        for (UUID playerUUID : new HashSet<>(playerPitchHistory.keySet())) {
            Player player = Bukkit.getPlayer(playerUUID);

            // 플레이어가 오프라인이면 추적을 중지하고 다음 플레이어로 넘어감
            if (player == null || !player.isOnline()) {
                stopTracking(playerUUID);
                continue;
            }

            // --- 데이터 수집 (Sliding Window 방식) ---
            // Pitch 데이터 수집
            LinkedList<Float> pitches = playerPitchHistory.get(playerUUID);
            pitches.add(player.getLocation().getPitch());
            if (pitches.size() > HeadMovementAnalyzer.ANALYSIS_WINDOW_SIZE) {
                pitches.removeFirst(); // 가장 오래된 데이터 제거
            }

            // Yaw 데이터 수집
            LinkedList<Float> yaws = playerYawHistory.get(playerUUID);
            yaws.add(player.getLocation().getYaw());
            if (yaws.size() > HeadMovementAnalyzer.ANALYSIS_WINDOW_SIZE) {
                yaws.removeFirst(); // 가장 오래된 데이터 제거
            }

            // 쿨다운 상태이면 분석을 건너뜀
            if (isPlayerOnCooldown(playerUUID)) {
                continue;
            }

            // 분석을 위한 데이터가 충분히 쌓였는지 확인
            if (pitches.size() >= HeadMovementAnalyzer.ANALYSIS_WINDOW_SIZE) {
                runAnalysis(player, pitches, yaws);
            }
        }
    }

    /**
     * 수집된 데이터를 바탕으로 제스처 분석을 실행합니다.
     */
    private void runAnalysis(Player player, List<Float> pitchData, List<Float> yawData) {
        // 끄덕임(Nod) 분석 먼저 시도
        if (HeadMovementAnalyzer.isNod(pitchData)) {
            // 커스텀 NodEvent 발생
            Bukkit.getPluginManager().callEvent(new NodEvent(player));
            // 제스처가 감지되었으므로 데이터 초기화 및 쿨다운 설정
            setCooldownAndClearHistory(player.getUniqueId());
            return; // 끄덕임이 감지되면 젓기 분석은 건너뛰어 중복 감지 방지
        }

        // 젓기(Shake) 분석 시도
        if (HeadMovementAnalyzer.isShake(yawData)) {
            // 커스텀 ShakeEvent 발생
            Bukkit.getPluginManager().callEvent(new ShakeEvent(player));
            // 제스처가 감지되었으므로 데이터 초기화 및 쿨다운 설정
            setCooldownAndClearHistory(player.getUniqueId());
        }
    }

    /**
     * 특정 플레이어의 머리 움직임 추적을 시작합니다.
     * @param player 추적할 플레이어
     */
    public void startTracking(Player player) {
        UUID uuid = player.getUniqueId();
        // 데이터 저장소에 플레이어 추가 및 빈 리스트 초기화
        playerPitchHistory.put(uuid, new LinkedList<>());
        playerYawHistory.put(uuid, new LinkedList<>());
    }

    /**
     * 특정 플레이어의 머리 움직임 추적을 중지하고 관련 데이터를 모두 삭제합니다.
     * @param playerUUID 추적을 중지할 플레이어의 UUID
     */
    public void stopTracking(UUID playerUUID) {
        playerPitchHistory.remove(playerUUID);
        playerYawHistory.remove(playerUUID);
        gestureCooldowns.remove(playerUUID);
    }

    // 플레이어가 접속하면 추적을 시작합니다.
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        startTracking(event.getPlayer());
    }

    // 플레이어가 퇴장하면 데이터를 정리하여 메모리 누수를 방지합니다.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopTracking(event.getPlayer().getUniqueId());
    }

    // 플레이어가 쿨다운 상태인지 확인
    private boolean isPlayerOnCooldown(UUID uuid) {
        return gestureCooldowns.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }

    // 제스처 감지 후 데이터 기록을 초기화하고 쿨다운을 설정
    private void setCooldownAndClearHistory(UUID uuid) {
        // 데이터 리스트를 비워, 같은 움직임으로 이벤트가 연속 발생하는 것을 방지
        if (playerPitchHistory.containsKey(uuid)) {
            playerPitchHistory.get(uuid).clear();
        }
        if (playerYawHistory.containsKey(uuid)) {
            playerYawHistory.get(uuid).clear();
        }
        // 현재 시간 + 쿨다운 시간으로 만료 시간 설정
        gestureCooldowns.put(uuid, System.currentTimeMillis() + GESTURE_COOLDOWN_MS);
    }
}