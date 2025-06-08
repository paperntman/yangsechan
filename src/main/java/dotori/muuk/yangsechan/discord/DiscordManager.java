package dotori.muuk.yangsechan.discord;// DiscordManager.java
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordManager {

    private JDA jda;
    private final JavaPlugin plugin;
    private final LinkManager linkManager;

    public DiscordManager(JavaPlugin plugin, LinkManager linkManager) {
        this.plugin = plugin;
        this.linkManager = linkManager;
    }

    public void initialize() {
        String token = plugin.getConfig().getString("discord-bot-token");
        if (token == null || token.equals("YOUR_DISCORD_BOT_TOKEN_HERE")) {
            plugin.getLogger().warning("디스코드 봇 토큰이 설정되지 않았습니다! 디스코드 연동 기능이 비활성화됩니다.");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .setActivity(Activity.playing("양세찬 게임"))
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT) // 메시지 내용을 읽기 위한 인텐트
                    .addEventListeners(new DiscordCommandListener(linkManager)) // 명령어 리스너 등록
                    .build();

            jda.awaitReady(); // 봇이 완전히 준비될 때까지 대기
            plugin.getLogger().info("디스코드 봇이 성공적으로 연결되었습니다.");

            // 슬래시 명령어 등록
            jda.upsertCommand("연동", "마인크래프트 계정을 연동합니다.")
                    .addOption(OptionType.STRING, "코드", "마인크래프트에서 받은 인증 코드를 입력하세요.", true)
                    .queue();

        } catch (InterruptedException e) {
            plugin.getLogger().severe("디스코드 봇 연결 중 오류 발생: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            plugin.getLogger().info("디스코드 봇 연결이 종료되었습니다.");
        }
    }

    public LinkManager getLinkManager() {
        return linkManager;
    }

    // 음소거/해제 메서드 추가
    public void mutePlayer(long discordUserId) {
        // ... JDA를 사용하여 음성 채널에서 해당 유저를 서버 음소거하는 로직 ...
    }

    public void unmutePlayer(long discordUserId) {
        // ... 서버 음소거를 해제하는 로직 ...
    }
}