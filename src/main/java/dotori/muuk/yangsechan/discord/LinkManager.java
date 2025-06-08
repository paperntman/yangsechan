package dotori.muuk.yangsechan.discord;// LinkManager.java
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LinkManager {

    private final JavaPlugin plugin;
    private final File configFile;
    private final FileConfiguration dataConfig;

    // 임시 인증 코드 저장소 <Minecraft UUID, Auth Code>
    private final Map<UUID, String> authCodes = new ConcurrentHashMap<>();

    // 영구 연동 데이터 저장소 <Minecraft UUID, Discord ID>
    private final Map<UUID, Long> linkedAccounts = new ConcurrentHashMap<>();

    public LinkManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "discord-data.yml");
        if (!configFile.exists()) {
            plugin.saveResource("discord-data.yml", false);
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(configFile);
        loadData();
    }

    // 새로운 인증 코드를 생성하고 반환
    public String generateAuthCode(UUID playerUuid) {
        // 랜덤 한글 단어 3개 생성 로직
        String[] words = {"가마우지", "갈까마귀", "개개비", "개똥지빠귀", "개꿩", "고니", "곤줄박이", "갈매기", "까치", "꾀꼬리", "꿩", "논병아리", "노랑할미새", "동고비", "두견이", "두루미", "독수리", "딱새", "따오기", "뜸부기", "때까치", "매", "멧새", "멧비둘기", "물닭", "물총새", "물까치", "박새", "방울새", "백할미새", "뱁새", "뻐꾸기", "비둘기", "비오리", "부엉이", "뻐꾸기", "사랑새", "상모솔새", "새홀리기", "새매", "소쩍새", "솔개", "솔부엉이", "수리부엉이", "쑥새", "알락할미새", "앵무새", "양진이", "어치", "오목눈이", "오리", "오색딱다구리", "올빼미", "왜가리", "원앙", "원앙사촌", "유리딱새", "저어새", "제비", "조롱이", "종다리", "직박구리", "진박새", "참매", "참새", "청둥오리", "청딱다구리", "칡부엉이", "큰고니", "큰기러기", "큰오색딱다구리", "팔색조", "파랑새", "할미새", "해오라기", "호반새", "호랑지빠귀", "황새", "황조롱이", "후투티", "휘파람새", "흰뺨검둥오리", "흰꼬리수리", "흰배지빠귀", "흰죽지", "학", "할미새사촌", "호사비오리", "황오리", "흑두루미", "흥부새", "흰머리오목눈이", "흰눈썹황금새", "흰기러기", "흰꼬리딱새", "홍부리황새", "홍학", "화작", "황여새", "황금새", "흑기러기"};
        Random random = new Random();
        String code = words[random.nextInt(words.length)] + " " +
                      words[random.nextInt(words.length)] + " " +
                      words[random.nextInt(words.length)];
        
        authCodes.put(playerUuid, code);
        return code;
    }

    // 코드를 사용하여 계정 연동 시도
    public boolean tryLinkAccount(long discordId, String code) {
        for (Map.Entry<UUID, String> entry : authCodes.entrySet()) {
            if (entry.getValue().equals(code)) {
                UUID playerUuid = entry.getKey();
                linkedAccounts.put(playerUuid, discordId); // 연동 정보 저장
                authCodes.remove(playerUuid); // 임시 코드 삭제
                saveData(); // 파일에 저장
                return true;
            }
        }
        return false;
    }
    
    public Long getDiscordId(UUID playerUuid) {
        return linkedAccounts.get(playerUuid);
    }

    // 파일에서 데이터 로드
    private void loadData() {
        if (dataConfig.getConfigurationSection("linked-accounts") != null) {
            for (String uuidStr : dataConfig.getConfigurationSection("linked-accounts").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                long discordId = dataConfig.getLong("linked-accounts." + uuidStr);
                linkedAccounts.put(uuid, discordId);
            }
        }
    }

    // 파일에 데이터 저장
    private void saveData() {
        // 기존 데이터 삭제 후 새로 쓰기
        dataConfig.set("linked-accounts", null);
        linkedAccounts.forEach((uuid, discordId) -> dataConfig.set("linked-accounts." + uuid.toString(), discordId));
        try {
            dataConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("디스코드 연동 데이터를 저장하는 데 실패했습니다.");
        }
    }
}