package dotori.muuk.yangsechan.discord;// DiscordCommandListener.java (새 파일)
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Objects;

public class DiscordCommandListener extends ListenerAdapter {
    private final LinkManager linkManager;

    public DiscordCommandListener(LinkManager linkManager) {
        this.linkManager = linkManager;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("연동")) return;

        long discordId = event.getUser().getIdLong();
        String code = Objects.requireNonNull(event.getOption("코드")).getAsString();

        boolean success = linkManager.tryLinkAccount(discordId, code);

        if (success) {
            event.reply("✅ 마인크래프트 계정 연동에 성공했습니다!").setEphemeral(true).queue();
        } else {
            event.reply("❌ 유효하지 않은 인증 코드입니다. 마인크래프트에서 `/연동` 명령어를 다시 실행해주세요.").setEphemeral(true).queue();
        }
    }
}